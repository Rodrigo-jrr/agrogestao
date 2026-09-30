package com.agrogestao.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrogestao.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

class DataExportTest extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void exportHasAccountConsentsAndOwnedPropertyData() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        post("/api/activities", owner, Map.of("name", "Horta", "type", "HORTA"));
        Session other = registerOwnerWithProperty("Vizinha");
        addMember(other, owner, "WORKER");

        JsonNode export = get("/api/me/export", owner);

        assertThat(export.get("conta").get("email").asString()).isEqualTo(owner.email());
        assertThat(export.get("aceitesDosTermos")).hasSize(1);
        JsonNode properties = export.get("propriedades");
        assertThat(properties).hasSize(2);
        JsonNode mine = find(properties, "Sítio Dona");
        assertThat(mine.get("seuPapel").asString()).isEqualTo("Proprietário");
        assertThat(mine.get("dados").get("atividades").toString()).contains("Horta");
        assertThat(mine.get("dados").get("equipe").get(0).get("email").asString()).isEqualTo(owner.email());
        // Onde é colaboradora, não leva os dados da propriedade de outra pessoa.
        assertThat(find(properties, "Sítio Vizinha").get("dados").isString()).isTrue();
        assertThat(export.get("suasAcoesNoHistorico").toString()).contains("Horta");
        assertThat(export.toString()).doesNotContain("password").doesNotContain("token_hash");
    }

    @Test
    void exportIsDownloadedAsAFileAndAudited() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/me/export").header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("attachment; filename=\"agrogestao-meus-dados-")));
        Long userId = get("/api/me", owner).get("userId").asLong();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE user_id = ? AND action = 'EXPORT'", Integer.class, userId))
                .isEqualTo(1);
    }

    /** Toda tabela com property_id aparece em alguma seção da exportação. */
    @Test
    void everyPropertyTableIsExported() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.columns WHERE LOWER(column_name) = 'property_id' "
                        + "AND LOWER(table_schema) = 'public'", String.class);
        String allSql = String.join(" ", DataExportService.PROPERTY_SECTIONS.values());
        for (String table : tables) {
            assertThat(allSql).as("tabela %s na exportação", table).containsPattern("FROM " + table + "\\b");
        }
    }

    private static JsonNode find(JsonNode properties, String name) {
        for (JsonNode property : properties) {
            if (property.get("nome").asString().equals(name)) {
                return property;
            }
        }
        throw new AssertionError("propriedade " + name + " não exportada");
    }
}
