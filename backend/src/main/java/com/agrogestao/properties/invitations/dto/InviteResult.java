package com.agrogestao.properties.invitations.dto;

import com.agrogestao.properties.dto.MemberResponse;

/**
 * ADDED: a pessoa já tinha conta e entrou direto na equipe ({@code member}).
 * INVITED: ainda não tinha conta; recebeu um convite por e-mail ({@code invitation}).
 */
public record InviteResult(Outcome outcome, MemberResponse member, InvitationResponse invitation) {

    public enum Outcome { ADDED, INVITED }
}
