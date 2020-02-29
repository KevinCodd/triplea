package games.strategy.engine.data.loader;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Level;
import lombok.Builder;
import lombok.extern.java.Log;

@Builder
@Log
public class GameLoader {
  // various error handling callbacks


  public Optional<GameData> loadGame(final File file) {
    if (!file.isFile()) {
      return Optional.empty();
    }

    final GameData newData;
    try {
      // if the file name is xml, load it as a new game
      if (file.getName().toLowerCase().endsWith("xml")) {
        try (InputStream inputStream = new FileInputStream(file)) {
          newData = GameParser.parse(file.getAbsolutePath(), inputStream);
        }
      } else {
        // try to load it as a saved game whatever the extension
        newData = loadGame(file);
      }
      return Optional.of(newData);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return Optional.empty();
    }
  }

  /**
   * Loads game data from the specified file.
   *
   * @param file The file from which the game data will be loaded.
   * @return The loaded game data.
   * @throws IOException If an error occurs while loading the game.
   */
  public static GameData loadGame(final File file) throws IOException {
    checkNotNull(file);

    try (InputStream fis = new FileInputStream(file);
         InputStream is = new BufferedInputStream(fis)) {
      return GameDataManager.loadGame(is);
    }
  }

}
