package ar.edu.utn.frc.tup.piii.judge.resolvers;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import java.util.List;

public interface JudgeQueryResolver {

    JudgeQueryCategory supports();

    JudgeResponseDTO resolve(JudgeQueryDTO query, MatchStateDTO matchState, List<JudgeEvent> log);
}
