package org.hbmc;

import org.hbmc.config.DatabaseInitializer;
import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Room;
import org.hbmc.model.User;
import org.hbmc.model.enums.RoomType;

import java.math.BigDecimal;

public class Main {
    static void main() {
        DatabaseInitializer.initialize();
        DatabaseConnection.getInstance();
    }
}
