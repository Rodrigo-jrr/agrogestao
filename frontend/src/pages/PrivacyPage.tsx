import { LegalField, LegalLayout, LegalSection } from '../components/LegalLayout'
import { LEGAL_ENTITY } from '../lib/legal'

const listClass = 'list-disc space-y-1 pl-5'

export function PrivacyPage() {
  return (
    <LegalLayout title="Política de privacidade">
      <p>
        Esta política explica quais dados o AgroGestão guarda, para quê, com quem compartilha e como você exerce seus
        direitos, conforme a Lei Geral de Proteção de Dados (Lei 13.709/2018, LGPD).
      </p>

      <LegalSection title="1. Quem é o responsável">
        <p>
          O controlador dos dados da sua conta é <LegalField value={LEGAL_ENTITY.controller} />, com endereço em{' '}
          <LegalField value={LEGAL_ENTITY.address} />. Para falar sobre privacidade, inclusive com o encarregado de dados,
          escreva para <LegalField value={LEGAL_ENTITY.privacyEmail} />.
        </p>
      </LegalSection>

      <LegalSection title="2. Quais dados guardamos">
        <ul className={listClass}>
          <li>
            <strong className="font-medium text-terra">Da sua conta:</strong> nome, e-mail e senha. A senha é guardada de
            forma irreversível (hash); ninguém consegue lê-la, nem nós.
          </li>
          <li>
            <strong className="font-medium text-terra">Do seu aceite:</strong> a versão destes documentos que você aceitou,
            a data e o endereço IP de onde aceitou.
          </li>
          <li>
            <strong className="font-medium text-terra">Das suas propriedades:</strong> o que você e sua equipe
            registram — lançamentos, contas, estoque, compras, vendas, produção, atividades, clientes e fornecedores.
          </li>
          <li>
            <strong className="font-medium text-terra">Histórico de alterações:</strong> quem criou, alterou ou excluiu
            cada registro, e quando.
          </li>
          <li>
            <strong className="font-medium text-terra">Segurança:</strong> endereço IP e tentativas de login, usados só
            para barrar ataques e mantidos por pouco tempo.
          </li>
        </ul>
        <p>
          No seu navegador, guardamos apenas o que é necessário para você continuar conectado e lembrar a propriedade
          selecionada. Não usamos cookies de publicidade nem de rastreamento.
        </p>
      </LegalSection>

      <LegalSection title="3. Para que usamos e com qual base legal">
        <ul className={listClass}>
          <li>Prestar o serviço e enviar e-mails da conta (confirmação, senha, convites): execução de contrato.</li>
          <li>Proteger contas e o sistema contra fraudes e acessos indevidos: legítimo interesse.</li>
          <li>Comprovar o seu aceite e cumprir obrigações legais: cumprimento de obrigação legal e exercício de direitos.</li>
        </ul>
        <p>Não vendemos seus dados e não os usamos para publicidade.</p>
      </LegalSection>

      <LegalSection title="4. Dados de outras pessoas que você registra">
        <p>
          Quando você registra clientes, fornecedores ou membros da equipe, você decide o que registrar e para quê. Nesse
          caso, nós tratamos esses dados em seu nome, apenas para prestar o serviço.
        </p>
      </LegalSection>

      <LegalSection title="5. Com quem compartilhamos">
        <ul className={listClass}>
          <li>Com as pessoas da equipe de cada propriedade, conforme o papel que o proprietário deu a elas.</li>
          <li>
            Com empresas que nos ajudam a operar o serviço, como hospedagem e envio de e-mails, que só podem usar os dados
            para isso.
          </li>
          <li>Com autoridades, quando a lei ou uma ordem judicial exigir.</li>
        </ul>
      </LegalSection>

      <LegalSection title="6. Por quanto tempo">
        <p>
          Guardamos seus dados enquanto sua conta existir. Ao excluir a conta, apagamos na hora seus dados pessoais e as
          propriedades em que você era a única pessoa da equipe. Nas propriedades que continuam com outras pessoas, os
          registros que você fez permanecem, mas o seu nome e e-mail são retirados do histórico. Cópias de segurança são
          apagadas em até <LegalField value={LEGAL_ENTITY.backupRetention} />.
        </p>
      </LegalSection>

      <LegalSection title="7. Seus direitos">
        <p>A LGPD garante a você, entre outros, os direitos de:</p>
        <ul className={listClass}>
          <li>
            confirmar que tratamos seus dados e acessá-los — em Meu perfil, <em>Baixar meus dados</em> gera um arquivo
            com tudo;
          </li>
          <li>corrigir dados incompletos ou errados — em Meu perfil você altera nome, e-mail e senha;</li>
          <li>levar seus dados para outro serviço (portabilidade) — o mesmo arquivo de <em>Baixar meus dados</em>;</li>
          <li>pedir a eliminação — em Meu perfil, <em>Excluir minha conta</em>;</li>
          <li>saber com quem compartilhamos e revogar consentimentos, pelo contato da seção 1.</li>
        </ul>
        <p>
          Você também pode reclamar à Autoridade Nacional de Proteção de Dados (ANPD), mas gostaríamos de resolver com você
          primeiro.
        </p>
      </LegalSection>

      <LegalSection title="8. Segurança">
        <p>
          Usamos conexão criptografada, senhas guardadas com hash, links de uso único para confirmação e recuperação de
          conta, isolamento entre propriedades e bloqueio temporário após tentativas repetidas de login.
        </p>
      </LegalSection>

      <LegalSection title="9. Menores de idade">
        <p>O AgroGestão não é destinado a menores de 18 anos.</p>
      </LegalSection>

      <LegalSection title="10. Mudanças nesta política">
        <p>
          Quando esta política mudar, a data no topo será atualizada e pediremos que você leia e aceite a nova versão no
          próximo acesso.
        </p>
      </LegalSection>
    </LegalLayout>
  )
}
