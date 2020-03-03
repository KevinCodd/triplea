package games.strategy.engine.data.loader;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.GameDataManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingConsumer;
import org.triplea.util.Version;

@Builder
@NoArgsConstructor
@Log
public class GameLoader {

  @Builder.Default
  private ThrowingConsumer<String, IOException> errorHandler =
      err -> {
        throw new IOException(err);
      };

  @AllArgsConstructor
  public static class EngineIncompatibleWithNewerSave extends Exception {
    private final String versionFound;
  }

  @AllArgsConstructor
  public static class EngineIncompatibleWithOlderSave extends Exception {
    private final String versionFound;
  }

  public static class EngineIncompatibleWithSave extends Exception {
    private EngineIncompatibleWithSave(final Exception cause) {
      log.log(Level.INFO, "Unable to determine game version of save game", cause);
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
  public GameData loadGame(final InputStream is) throws IOException {
    checkNotNull(is);

    final ObjectInputStream input = new ObjectInputStream(new GZIPInputStream(is));
    try {
      final Object readVersion = input.readObject();

      if (readVersion instanceof Version) {
        if (!ClientContext.engineVersion().isCompatibleWithEngineVersion((Version) readVersion)) {
          if (ClientContext.engineVersion().isGreaterThan((Version) readVersion)) {
            throw new EngineIncompatibleWithNewerSave(readVersion.toString());
          }
        }
      } else if (readVersion instanceof games.strategy.util.Version) {
        throw new EngineIncompatibleWithOlderSave(
            ((games.strategy.util.Version) readVersion).getExactVersion());
      }

      final GameData data = (GameData) input.readObject();
      data.postDeSerialize();
      loadDelegates(input, data);
      return data;
    } catch (final Exception e) {
      throw new IOException(e.getMessage());
    }
  }

  private static void loadDelegates(final ObjectInputStream input, final GameData data)
      throws Exception {
    for (Object endMarker = input.readObject();
        !endMarker.equals(GameDataManager.DELEGATE_LIST_END);
        endMarker = input.readObject()) {
      final String name = (String) input.readObject();
      final String displayName = (String) input.readObject();
      final String className = (String) input.readObject();
      final IDelegate instance;
      instance =
          Class.forName(className)
              .asSubclass(IDelegate.class)
              .getDeclaredConstructor()
              .newInstance();
      instance.initialize(name, displayName);
      data.addDelegate(instance);
      final String next = (String) input.readObject();
      if (next.equals(GameDataManager.DELEGATE_DATA_NEXT)) {
        instance.loadState((Serializable) input.readObject());
      }
    }
  }
}
