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

        try (InputStream fis = new FileInputStream(file);
             InputStream is = new BufferedInputStream(fis)) {
          newData = GameDataManager.loadGame(is);
        }
      }
      return Optional.of(newData);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return Optional.empty();
    }
  }
}
