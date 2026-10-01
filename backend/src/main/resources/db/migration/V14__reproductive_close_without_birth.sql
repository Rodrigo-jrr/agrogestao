-- Bugfix (QA): uma gestação em aberto (diagnóstico de prenhez positivo) também termina quando o animal sai
-- do rebanho (venda, morte, descarte) sem que tenha havido parto/aborto. Até aqui, encerrar uma gestação só
-- era possível criando outro evento (PARTO/ABORTO) com diagnosis_event_id apontando pro diagnóstico — o que
-- aqui não se aplica (não houve parto nem aborto, e inventar um kind pra isso mentiria a causa).
--
-- Guarda isso na própria linha do diagnóstico: encerramento sem nascimento não é um evento novo na linha do
-- tempo do animal (a saída em si já é o evento auditado), só o fim dessa gestação específica.
ALTER TABLE reproductive_events ADD COLUMN closed_without_birth_at DATETIME NULL;
