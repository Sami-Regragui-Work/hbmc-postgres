package org.hbmc;

import org.hbmc.model.Room;
import org.hbmc.model.User;
import org.hbmc.model.enums.RoomType;

import java.math.BigDecimal;

public class Main {
    static void main() {
        new Room("101", RoomType.SINGLE, 1, new BigDecimal("500.00"));
        System.out.println();
    }
}
