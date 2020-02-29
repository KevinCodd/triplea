package games.strategy.engine.framework;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.GameData;
import java.io.OutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GameDataManagerTest {
  @Nested
  final class SaveGameTest {
    @Test
    void shouldCloseOutputStream() throws Exception {
      final OutputStream os = mock(OutputStream.class);

      GameDataManager.saveGame(os, new GameData());

      verify(os).close();
    }
  }
}
