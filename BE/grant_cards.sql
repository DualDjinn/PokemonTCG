INSERT INTO player_card (player_id, card_id, quantity)
SELECT p.id, c.id, 4
FROM player p, card c
ON DUPLICATE KEY UPDATE quantity = quantity + 4;
