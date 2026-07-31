import { Injectable } from '@angular/core';

/**
 * Traduce los mensajes de error crudos del motor de juego (en inglés y técnicos)
 * a explicaciones claras de reglas en español, para que el juez Mew "sepa las
 * reglas a fondo" y avise de forma comprensible cada jugada no permitida.
 *
 * <p>El motor de reglas permanece puro y agnóstico de idioma (lanza mensajes en
 * inglés); la capa de presentación (FE) es la responsable de localizar y explicar
 * esos mensajes al jugador. Por eso este mapeo vive en el frontend.
 *
 * <p>El emparejamiento es por subcadena (`includes`) contra el mensaje del motor,
 * y el orden importa: las reglas más específicas van primero para no ser
 * "tapadas" por una genérica (p. ej. "Bench is full, cannot retreat" antes que
 * "Bench is full"). Si ningún patrón coincide, se devuelve un texto genérico para
 * no exponer el mensaje técnico en inglés.
 */
@Injectable({ providedIn: 'root' })
export class JudgeRulesExplainer {
  private readonly rules: ReadonlyArray<{ match: string; es: string }> = [
    // — Retirada (antes que el genérico "Bench is full") —
    { match: 'No active Pokemon to retreat', es: 'No tenés un Pokémon activo para retirar.' },
    { match: 'Bench is full, cannot retreat', es: 'No podés retirar: tu banca está llena (máximo 5 Pokémon).' },
    { match: 'No bench Pokemon to replace', es: 'No tenés Pokémon en la banca para poner como nuevo activo.' },
    { match: 'Cannot retreat right now', es: 'No podés retirar ahora: te falta energía para pagar el costo de retirada, o tu Pokémon está Dormido o Paralizado.' },

    // — Energía —
    { match: 'Energy cannot be attached', es: 'Solo podés asignar 1 energía por turno, y durante tu fase principal. Probablemente ya asignaste tu energía de este turno.' },
    { match: 'not an Energy', es: 'Esa carta no es una energía.' },

    // — Ataque (incluye "energía insuficiente") —
    { match: 'Attack cannot be used', es: 'No podés usar ese ataque ahora. Revisá que tengas la energía suficiente para su costo, que sea tu fase de ataque y que tu Pokémon no esté Dormido ni Paralizado.' },
    { match: 'no attacks available', es: 'Tu Pokémon activo no tiene ataques disponibles.' },
    { match: 'Active Pokemon has no attacks', es: 'Tu Pokémon activo no tiene ataques disponibles.' },

    // — Pokémon Básicos / banca —
    { match: 'Basic Pokemon cannot be played', es: 'Solo podés bajar Pokémon Básicos a la banca durante tu fase principal, y siempre que la banca no esté llena (máximo 5).' },
    { match: 'not a Basic Pokemon', es: 'Esa carta no es un Pokémon Básico.' },
    { match: 'Bench is full', es: 'Tu banca está llena: el máximo es 5 Pokémon.' },

    // — Evolución —
    { match: 'evolve on the first turn', es: 'No podés evolucionar en el primer turno de la partida.' },
    { match: 'does not evolve from', es: 'Esa carta no evoluciona de ese Pokémon.' },
    { match: 'was played this turn', es: 'No podés evolucionar un Pokémon el mismo turno en que lo jugaste.' },
    { match: 'already evolved this turn', es: 'Ese Pokémon ya evolucionó este turno: solo se permite una evolución por turno y por Pokémon.' },

    // — Entrenadores (la herramienta va antes que el genérico "not a Pokemon") —
    { match: 'An Item cannot be played', es: 'Solo podés jugar cartas de Objeto (Item) durante tu fase principal.' },
    { match: 'not an Item', es: 'Esa carta no es un Objeto (Item).' },
    { match: 'Supporter cannot be played', es: 'Solo podés jugar 1 carta de Partidario (Supporter) por turno, en tu fase principal.' },
    { match: 'not a Supporter', es: 'Esa carta no es un Partidario (Supporter).' },
    { match: 'Stadium cannot be played', es: 'No podés jugar ese Estadio ahora: quizá ya hay un Estadio con el mismo nombre en juego.' },
    { match: 'not a Stadium', es: 'Esa carta no es un Estadio.' },
    { match: 'Tool cannot be attached', es: 'Solo podés anexar 1 herramienta por Pokémon, durante tu fase principal.' },
    { match: 'not a Pokemon Tool', es: 'Esa carta no es una herramienta Pokémon.' },

    // — Genérico de Pokémon (después de Tool/Basic para no tapar a esos) —
    { match: 'not a Pokemon', es: 'Esa carta no es un Pokémon.' },

    // — Habilidades —
    { match: 'Ability already used this turn', es: 'Esa habilidad ya se usó este turno.' },
    { match: 'is not an activated ability', es: 'Esa habilidad no se puede activar manualmente.' },
    { match: 'Ability not found', es: 'Ese Pokémon no tiene esa habilidad.' },

    // — Turno / fase / setup / premios —
    { match: 'must have an active Pokemon before acting', es: 'Ambos jugadores deben tener un Pokémon activo antes de empezar a jugar.' },
    { match: 'waiting for active Pokemon selection', es: 'La partida está esperando que elijas tu Pokémon activo.' },
    { match: 'Selected active Pokemon must be Basic', es: 'Tu Pokémon activo debe ser un Pokémon Básico.' },
    { match: 'Selected bench Pokemon must be Basic', es: 'El Pokémon que pongas en la banca debe ser un Básico.' },
    { match: 'select an active Pokemon first', es: 'Primero tenés que elegir tu Pokémon activo.' },
    { match: 'does not need to choose an active', es: 'Ya tenés tu Pokémon activo elegido.' },
    { match: 'is not pending prize selection', es: 'No es momento de tomar cartas de premio.' },
    { match: 'Invalid prize index', es: 'Esa carta de premio no es válida.' },
    { match: 'beneficiary can resolve mulligan', es: 'Solo el jugador beneficiado puede robar por los mulligans.' },
    { match: 'Coin side must be', es: 'Tenés que elegir cara o cruz.' },
    { match: 'opening chooser can pick', es: 'No es tu turno de elegir en el sorteo.' },
    { match: 'current chooser can decide the starter', es: 'No es tu turno de decidir quién empieza.' },
    { match: 'Not authenticated', es: 'Tu sesión expiró. Volvé a iniciar sesión.' },
  ];

  private readonly fallback = 'Esa jugada no está permitida en este momento.';

  /**
   * Devuelve la explicación de regla en español correspondiente al mensaje crudo
   * del motor, o un texto genérico si no hay un patrón conocido.
   *
   * @param rawMessage mensaje de error del backend (puede ser null/undefined)
   * @returns explicación clara en español para mostrar en el bocadillo de Mew
   */
  explain(rawMessage: string | null | undefined): string {
    if (!rawMessage) {
      return this.fallback;
    }
    const found = this.rules.find(rule => rawMessage.includes(rule.match));
    return found ? found.es : this.fallback;
  }
}
