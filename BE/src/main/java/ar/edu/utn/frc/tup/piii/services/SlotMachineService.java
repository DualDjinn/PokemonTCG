package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.slot.SlotSpinResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotMachineService {

    private final PlayerRepository playerRepository;

    public SlotMachineService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public SlotSpinResponse spin(String playerId, int bet, int winAmount) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (player.getCoins() < bet) {
            throw new IllegalStateException("Insufficient coins");
        }

        int newCoins = player.getCoins() - bet + winAmount;
        player.setCoins(newCoins);
        playerRepository.save(player);

        return new SlotSpinResponse(newCoins);
    }
}
