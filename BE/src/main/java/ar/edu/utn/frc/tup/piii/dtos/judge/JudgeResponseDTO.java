package ar.edu.utn.frc.tup.piii.dtos.judge;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeResponseDTO {

    private String queryId;
    private JudgeQueryCategory category;
    private String summary;
    private List<String> reasons;
    private List<String> ruleReferences;
    private String suggestion;
    private boolean hasActiveRestrictions;
    private int turnNumber;
    private String lang;
}
