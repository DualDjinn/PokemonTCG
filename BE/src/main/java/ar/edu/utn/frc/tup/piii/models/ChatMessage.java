package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDateTime;

public record ChatMessage(
    String matchId,
    String senderId,
    String senderName,
    String content,
    LocalDateTime timestamp
) {}
