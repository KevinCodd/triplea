package games.strategy.engine.data.loader;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.triplea.UrlConstants;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.util.Version;

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
          newData = loadGame(is);
        }
      }
      return Optional.of(newData);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return Optional.empty();
    }
  }


  /**
   * Loads game data from the specified stream.
   *
   * @param is The stream from which the game data will be loaded. The caller is responsible for
   *     closing this stream; it will not be closed when this method returns.
   * @return The loaded game data.
   * @throws IOException If an error occurs while loading the game.
   */
  public static GameData loadGame(final InputStream is) throws IOException {
    checkNotNull(is);

    final ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(is));
    try {
      final Version readVersion = (Version) input.readObject();
      if (!ClientContext.engineVersion().isCompatibleWithEngineVersion(readVersion)) {
        final String error =
            "Incompatible engine versions. We are: "
                + ClientContext.engineVersion()
                + " . Trying to load game created with: "
                + readVersion
                + "\nTo download the latest version of TripleA, Please visit "
                + UrlConstants.DOWNLOAD_WEBSITE;
        throw new IOException(error);
      }

      final GameData data = (GameData) input.readObject();
      data.postDeSerialize();
      loadDelegates(input, data);
      return data;
    } catch (final ClassNotFoundException cnfe) {
      throw new IOException(cnfe.getMessage());
    }
  }

  private static void loadDelegates(final ObjectInputStream input, final GameData data)
      throws ClassNotFoundException, IOException {
    for (Object endMarker = input.readObject();
         !endMarker.equals(GameDataManager.DELEGATE_LIST_END);
         endMarker = input.readObject()) {
      final String name = (String) input.readObject();
      final String displayName = (String) input.readObject();
      final String className = (String) input.readObject();
      final IDelegate instance;
      try {
        instance =
            Class.forName(className)
                .asSubclass(IDelegate.class)
                .getDeclaredConstructor()
                .newInstance();
        instance.initialize(name, displayName);
        data.addDelegate(instance);
      } catch (final Exception e) {
        throw new IOException(e);
      }
      final String next = (String) input.readObject();
      if (next.equals(GameDataManager.DELEGATE_DATA_NEXT)) {
        instance.loadState((Serializable) input.readObject());
      }
    }
  }

}
