package com.agrogestao.privacy;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.AttemptLimiter;
import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import com.agrogestao.auth.terms.TermsAcceptanceRepository;
import com.agrogestao.common.exception.TooManyRequestsException;
import com.agrogestao.properties.Property;
import com.agrogestao.properties.PropertyMember;
import com.agrogestao.properties.PropertyMemberRepository;
import com.agrogestao.properties.PropertyRepository;
import com.agrogestao.properties.PropertyRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Exportação dos dados do titular (LGPD, art. 18, V — portabilidade), em JSON:
 * dados da conta, aceites dos termos, vínculos com propriedades, as ações da pessoa no histórico e,
 * nas propriedades em que ela é proprietária, todos os dados da propriedade.
 */
@Service
public class DataExportService {

    /**
     * Seções exportadas de cada propriedade (propriedade = parâmetro). Ao criar uma tabela com property_id,
     * inclua-a aqui — o DataExportTest confere que nenhuma fica de fora.
     */
    static final Map<String, String> PROPERTY_SECTIONS = sections();

    private static Map<String, String> sections() {
        Map<String, String> s = new LinkedHashMap<>();
        s.put("atividades", "SELECT * FROM activities WHERE property_id = ? ORDER BY id");
        s.put("centrosDeCusto", "SELECT * FROM cost_centers WHERE property_id = ? ORDER BY id");
        s.put("lancamentosFinanceiros", "SELECT * FROM financial_transactions WHERE property_id = ? ORDER BY transaction_date, id");
        s.put("contasAPagar", "SELECT * FROM accounts_payable WHERE property_id = ? ORDER BY due_date, id");
        s.put("contasAReceber", "SELECT * FROM accounts_receivable WHERE property_id = ? ORDER BY due_date, id");
        s.put("fornecedores", "SELECT * FROM suppliers WHERE property_id = ? ORDER BY id");
        s.put("clientes", "SELECT * FROM customers WHERE property_id = ? ORDER BY id");
        s.put("produtos", "SELECT * FROM products WHERE property_id = ? ORDER BY id");
        s.put("saldosDeEstoque", "SELECT * FROM inventory_items WHERE property_id = ? ORDER BY id");
        s.put("movimentacoesDeEstoque", "SELECT * FROM inventory_movements WHERE property_id = ? ORDER BY movement_date, id");
        s.put("compras", "SELECT * FROM purchases WHERE property_id = ? ORDER BY purchase_date, id");
        s.put("itensDeCompra", "SELECT i.* FROM purchase_items i JOIN purchases p ON p.id = i.purchase_id "
                + "WHERE p.property_id = ? ORDER BY i.id");
        s.put("vendas", "SELECT * FROM sales WHERE property_id = ? ORDER BY sale_date, id");
        s.put("itensDeVenda", "SELECT i.* FROM sale_items i JOIN sales v ON v.id = i.sale_id "
                + "WHERE v.property_id = ? ORDER BY i.id");
        s.put("especies", "SELECT * FROM species WHERE property_id = ? ORDER BY id");
        s.put("lotesDeAnimais", "SELECT * FROM animal_batches WHERE property_id = ? ORDER BY id");
        s.put("animais", "SELECT * FROM animals WHERE property_id = ? ORDER BY id");
        s.put("comprasEVendasDeAnimais", "SELECT * FROM livestock_trades WHERE property_id = ? ORDER BY trade_date, id");
        s.put("sanidade", "SELECT * FROM health_applications WHERE property_id = ? ORDER BY application_date, id");
        s.put("eventosDoRebanho", "SELECT * FROM animal_events WHERE property_id = ? ORDER BY event_date, id");
        s.put("producao", "SELECT * FROM production_records WHERE property_id = ? ORDER BY production_date, id");
        s.put("equipe", "SELECT u.name, u.email, m.role, m.created_at FROM property_members m "
                + "JOIN users u ON u.id = m.user_id WHERE m.property_id = ? ORDER BY m.id");
        // Sem token_hash: o link do convite não sai do e-mail.
        s.put("convitesPendentes", "SELECT email, role, created_at, expires_at FROM property_invitations "
                + "WHERE property_id = ? ORDER BY id");
        s.put("historicoDeAlteracoes", "SELECT a.created_at, a.user_id, u.name AS user_name, a.entity_name, "
                + "a.entity_id, a.action, a.details FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id "
                + "WHERE a.property_id = ? ORDER BY a.id");
        return s;
    }

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    /** Exportar lê o banco inteiro da pessoa: poucas vezes por hora bastam. */
    private final AttemptLimiter exportsByUser = new AttemptLimiter(5, Duration.ofHours(1), Clock.systemUTC());

    public DataExportService(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyMemberRepository propertyMemberRepository,
            TermsAcceptanceRepository termsAcceptanceRepository,
            AuditService auditService,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.termsAcceptanceRepository = termsAcceptanceRepository;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> export(Long userId) {
        String key = "export:" + userId;
        if (exportsByUser.blockedFor(key).isPresent()) {
            throw new TooManyRequestsException("Você já exportou seus dados várias vezes na última hora. Tente mais tarde.");
        }
        exportsByUser.record(key);

        User user = userRepository.findById(userId).orElseThrow();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("sobre", "Exportação dos seus dados no AgroGestão (LGPD, art. 18, V). Valores em reais; "
                + "datas no horário de Brasília.");
        export.put("geradoEm", LocalDateTime.now().withNano(0).toString());

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("nome", user.getName());
        account.put("email", user.getEmail());
        account.put("emailConfirmadoEm", text(user.getEmailVerifiedAt()));
        account.put("emailPendenteDeConfirmacao", user.getPendingEmail());
        account.put("contaCriadaEm", text(user.getCreatedAt()));
        account.put("senhaAlteradaEm", text(user.getPasswordChangedAt()));
        export.put("conta", account);

        export.put("aceitesDosTermos", termsAcceptanceRepository.findByUserIdOrderByAcceptedAtAsc(userId).stream()
                .map(acceptance -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("versao", acceptance.getTermsVersion());
                    row.put("aceitoEm", text(acceptance.getAcceptedAt()));
                    row.put("enderecoIp", acceptance.getIpAddress());
                    return row;
                }).toList());

        export.put("propriedades", propertyMemberRepository.findByUserId(userId).stream()
                .map(membership -> propertySection(membership))
                .filter(section -> section != null)
                .toList());

        export.put("suasAcoesNoHistorico", rows("SELECT a.created_at, a.property_id, a.entity_name, a.entity_id, "
                + "a.action, a.details FROM audit_logs a WHERE a.user_id = ? ORDER BY a.id", userId));

        auditService.record(null, userId, "User", userId, AuditAction.EXPORT, "exportação dos dados pessoais");
        return export;
    }

    private Map<String, Object> propertySection(PropertyMember membership) {
        Property property = propertyRepository.findById(membership.getPropertyId()).orElse(null);
        if (property == null) {
            return null;
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("id", property.getId());
        section.put("nome", property.getName());
        section.put("localizacao", property.getLocation());
        section.put("areaTotalHa", property.getTotalAreaHa());
        section.put("seuPapel", membership.getRole().label());
        section.put("membroDesde", text(membership.getCreatedAt()));
        if (membership.getRole() == PropertyRole.OWNER) {
            Map<String, Object> data = new LinkedHashMap<>();
            PROPERTY_SECTIONS.forEach((name, sql) -> data.put(name, rows(sql, property.getId())));
            section.put("dados", data);
        } else {
            section.put("dados", "Só proprietários exportam os dados da propriedade. Peça a um proprietário.");
        }
        return section;
    }

    private List<Map<String, Object>> rows(String sql, Long parameter) {
        return jdbcTemplate.query(sql, ROW_MAPPER, parameter).stream().map(DataExportService::normalize).toList();
    }

    /**
     * Data e hora pelo getTimestamp: o driver aplica o fuso da conexão (serverTimezone=UTC) e devolve o mesmo
     * horário local que o Hibernate mostra no app. O getObject devolveria o valor cru, em UTC.
     */
    private static final ColumnMapRowMapper ROW_MAPPER = new ColumnMapRowMapper() {
        @Override
        protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
            int type = rs.getMetaData().getColumnType(index);
            if (type == Types.TIMESTAMP || type == Types.TIMESTAMP_WITH_TIMEZONE) {
                Timestamp timestamp = rs.getTimestamp(index);
                return timestamp == null ? null : timestamp.toLocalDateTime().withNano(0);
            }
            return super.getColumnValue(rs, index);
        }
    };

    /** Colunas em minúsculas e datas como texto ISO (o driver devolve tipos diferentes para datas). */
    private static Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((column, value) -> normalized.put(column.toLowerCase(), switch (value) {
            case Timestamp timestamp -> timestamp.toLocalDateTime().toString();
            case java.sql.Date date -> date.toLocalDate().toString();
            case TemporalAccessor temporal -> temporal.toString();
            case null -> null;
            default -> value;
        }));
        return normalized;
    }

    private static String text(LocalDateTime value) {
        return value == null ? null : value.withNano(0).toString();
    }
}
