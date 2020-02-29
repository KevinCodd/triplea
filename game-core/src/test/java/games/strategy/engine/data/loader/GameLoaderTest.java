package games.strategy.engine.data.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;

class GameLoaderTest {
  @Nested
  final class RoundTripTest {
    @Test
    void shouldPreserveGameName() throws Exception {
      final GameData data = new GameData();
      final byte[] bytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data));
      final GameData loaded = IoUtils.readFromMemory(bytes, GameLoader::loadGame);
      assertEquals(loaded.getGameName(), data.getGameName());
    }
  }
}
