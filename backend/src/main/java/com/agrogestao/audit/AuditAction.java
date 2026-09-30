package com.agrogestao.audit;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    CANCEL,
    VIEW,
    /** Exportação dos dados pessoais pelo titular (LGPD). */
    EXPORT
}
