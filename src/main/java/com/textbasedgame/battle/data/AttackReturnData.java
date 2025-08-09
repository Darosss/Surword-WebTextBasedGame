package com.textbasedgame.battle.data;

import javax.annotation.Nullable;

//TODO: latter add crit, deadly, two hit? etc
public record AttackReturnData(String id, AttackBase baseValues, boolean withDoubledAttack, @Nullable AttackDebuffs debuffs
) {


}
