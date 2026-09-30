import { Link } from 'react-router-dom'
import { LegalField, LegalLayout, LegalSection } from '../components/LegalLayout'
import { LEGAL_ENTITY } from '../lib/legal'

const listClass = 'list-disc space-y-1 pl-5'

export function TermsPage() {
  return (
    <LegalLayout title="Termos de uso">
      <p>
        Estes termos explicam as regras para usar o AgroGestão, um sistema de gestão para propriedades rurais
        oferecido por <LegalField value={LEGAL_ENTITY.controller} />. Ao criar uma conta, você concorda com eles e com a{' '}
        <Link to="/privacidade" className="text-rust underline-offset-2 hover:underline">
          Política de privacidade
        </Link>
        .
      </p>

      <LegalSection title="1. O serviço">
        <p>
          O AgroGestão ajuda a organizar o financeiro, o estoque, as compras, as vendas, a produção e a equipe de uma ou
          mais propriedades. Os números e relatórios são calculados a partir do que você e sua equipe registram.
        </p>
      </LegalSection>

      <LegalSection title="2. Sua conta">
        <ul className={listClass}>
          <li>Você precisa ter 18 anos ou mais e informar dados verdadeiros.</li>
          <li>
            Sua senha é pessoal. O que for feito com a sua conta é de sua responsabilidade; se suspeitar de uso indevido,
            troque a senha em Meu perfil.
          </li>
          <li>
            Quem convida pessoas para a equipe decide o papel de cada uma (proprietário, gerente, colaborador ou
            visualizador) e responde por esses acessos.
          </li>
        </ul>
      </LegalSection>

      <LegalSection title="3. Seus dados são seus">
        <p>
          Tudo o que você registra continua sendo seu. Nós usamos essas informações apenas para prestar o serviço, como
          descrito na Política de privacidade. Você pode baixar uma cópia dos seus dados ou excluir sua conta a qualquer
          momento, em Meu perfil.
        </p>
        <p>
          Se você registrar dados de outras pessoas (clientes, fornecedores, funcionários), é sua responsabilidade ter
          uma razão legítima para isso e tratá-los de acordo com a Lei Geral de Proteção de Dados (Lei 13.709/2018).
        </p>
      </LegalSection>

      <LegalSection title="4. Uso permitido">
        <p>Não é permitido:</p>
        <ul className={listClass}>
          <li>usar o AgroGestão para atividades ilegais ou para registrar informações de terceiros sem permissão;</li>
          <li>tentar acessar contas ou propriedades de outras pessoas, ou burlar as proteções do sistema;</li>
          <li>sobrecarregar o serviço de forma automatizada ou copiar o sistema para oferecê-lo a terceiros.</li>
        </ul>
      </LegalSection>

      <LegalSection title="5. Informações financeiras e fiscais">
        <p>
          O AgroGestão é uma ferramenta de organização. Ele não substitui contador, agrônomo ou consultor, e não emite
          documentos fiscais. Confira os valores antes de usá-los em declarações ou decisões importantes.
        </p>
      </LegalSection>

      <LegalSection title="6. Disponibilidade">
        <p>
          Trabalhamos para manter o serviço no ar e seus dados protegidos, mas pode haver interrupções para manutenção ou
          por falhas fora do nosso controle. Recomendamos baixar uma cópia dos seus dados de tempos em tempos.
        </p>
      </LegalSection>

      <LegalSection title="7. Preço">
        <p>
          Planos e preços, quando houver, serão informados antes de qualquer cobrança. Nada é cobrado sem o seu aceite.
        </p>
      </LegalSection>

      <LegalSection title="8. Encerramento">
        <p>
          Você pode excluir sua conta quando quiser. Podemos suspender uma conta que descumpra estes termos, avisando
          antes sempre que possível e permitindo que você baixe seus dados.
        </p>
      </LegalSection>

      <LegalSection title="9. Responsabilidade">
        <p>
          Na medida permitida pela lei, não respondemos por decisões tomadas com base em informações registradas de forma
          incorreta ou incompleta, nem por perdas causadas por uso da conta por pessoas a quem você deu acesso.
        </p>
      </LegalSection>

      <LegalSection title="10. Mudanças nestes termos">
        <p>
          Quando mudarmos estes termos, a data no topo será atualizada e pediremos que você leia e aceite a nova versão
          no próximo acesso.
        </p>
      </LegalSection>

      <LegalSection title="11. Lei e foro">
        <p>
          Estes termos seguem as leis brasileiras. Fica eleito o foro de <LegalField value={LEGAL_ENTITY.forum} />,
          ressalvado o direito do consumidor de usar o foro do seu domicílio.
        </p>
      </LegalSection>
    </LegalLayout>
  )
}
