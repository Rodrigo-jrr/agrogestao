-- Máquinas e manutenção, entrega 2: liga operação agrícola a uma máquina de verdade.
-- A coluna machine (texto livre, V16) continua existindo e intacta — histórico das operações já lançadas.
-- Só operações novas devem exigir machine_id; isso é validação do Back End, não constraint de banco, porque
-- linhas antigas legitimamente não têm máquina cadastrada.
ALTER TABLE crop_operations ADD COLUMN machine_id BIGINT NULL;
ALTER TABLE crop_operations ADD CONSTRAINT fk_crop_operation_machine
    FOREIGN KEY (machine_id) REFERENCES machines(id);

CREATE INDEX idx_crop_operations_machine ON crop_operations(machine_id);
