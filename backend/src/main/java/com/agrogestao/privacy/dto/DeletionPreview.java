package com.agrogestao.privacy.dto;

import java.util.List;

/**
 * O que acontece ao excluir a conta:
 * - toDelete: propriedades em que a pessoa está sozinha — apagadas com todos os dados;
 * - toLeave: propriedades com outros proprietários, ou em que ela não é proprietária — ela só sai da equipe;
 * - blocking: é a única proprietária, mas há outras pessoas na equipe — precisa passar a propriedade
 *   para alguém (torná-lo proprietário) ou excluí-la antes.
 */
public record DeletionPreview(List<PropertyRef> toDelete, List<PropertyRef> toLeave, List<PropertyRef> blocking) {

    public record PropertyRef(Long id, String name) {
    }

    public boolean canDelete() {
        return blocking.isEmpty();
    }
}
