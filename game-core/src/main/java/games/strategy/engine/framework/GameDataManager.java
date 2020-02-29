package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;

/** Responsible for loading saved games, new games from xml, and saving games. */
public final class GameDataManager {
  public static final String DELEGATE_DATA_NEXT = "<DelegateData>";
  public static final String DELEGATE_LIST_END = "<EndDelegateList>";
  private static final String DELEGATE_START = "<DelegateStart>";

  private GameDataManager() {}

  /**
   * Saves the specified game data to the specified stream.
   *
   * @param os The stream to which the game data will be saved. Note that this stream will be closed
   *     if this method returns successfully.
   * @param gameData The game data to save.
   * @throws IOException If an error occurs while saving the game.
   */
  public static void saveGame(final OutputStream os, final GameData gameData) throws IOException {
    checkNotNull(os);
    checkNotNull(gameData);

    saveGame(os, gameData, true);
  }

  static void saveGame(final OutputStream sink, final GameData data, final boolean saveDelegateInfo)
      throws IOException {
    final File tempFile =
        File.createTempFile(
            GameDataManager.class.getSimpleName(), GameDataFileUtils.getExtension());
    try {
      // write to temporary file first in case of error
      try (OutputStream os = new FileOutputStream(tempFile);
          OutputStream bufferedOutStream = new BufferedOutputStream(os);
          OutputStream zippedOutStream = new GZIPOutputStream(bufferedOutStream);
          ObjectOutputStream outStream = new ObjectOutputStream(zippedOutStream)) {
        outStream.writeObject(ClientContext.engineVersion());
        data.acquireReadLock();
        try {
          outStream.writeObject(data);
          if (saveDelegateInfo) {
            writeDelegates(data, outStream);
          } else {
            outStream.writeObject(DELEGATE_LIST_END);
          }
        } finally {
          data.releaseReadLock();
        }
      }

      // now write to sink (ensure sink is closed per method contract)
      try (InputStream is = new FileInputStream(tempFile);
          OutputStream os = new BufferedOutputStream(sink)) {
        IOUtils.copy(is, os);
      }
    } finally {
      tempFile.delete();
    }
  }

  private static void writeDelegates(final GameData data, final ObjectOutputStream out)
      throws IOException {
    for (final IDelegate delegate : data.getDelegates()) {
      out.writeObject(DELEGATE_START);
      // write out the delegate info
      out.writeObject(delegate.getName());
      out.writeObject(delegate.getDisplayName());
      out.writeObject(delegate.getClass().getName());
      out.writeObject(DELEGATE_DATA_NEXT);
      out.writeObject(delegate.saveState());
    }
    // mark end of delegate section
    out.writeObject(DELEGATE_LIST_END);
  }
}
