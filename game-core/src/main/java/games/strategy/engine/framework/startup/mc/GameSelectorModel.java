package games.strategy.engine.framework.startup.mc;

import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.loader.GameLoader;
import games.strategy.engine.data.loader.GameParser;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.GameChooserModel;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.settings.ClientSetting;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Observable;
import java.util.logging.Level;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * Model class that tracks the currently 'selected' game. This is the info that appears in the game
 * selector panel on the staging screens, eg: map, round, filename.
 */
@Log
public class GameSelectorModel extends Observable {
  @Nullable @Getter private GameData gameData = null;
  @Getter private String gameName = "";
  @Getter private String gameVersion = "";
  @Getter private String gameRound = "";
  @Getter private boolean canSelect = true;
  @Getter private boolean hostIsHeadlessBot = false;
  // just for host bots, so we can get the actions for loading/saving games on the bots from this
  // model
  @Setter @Getter private ClientModel clientModelForHostBots = null;

  public GameSelectorModel() {
    resetGameDataToNull();
  }

  public void resetGameDataToNull() {
    setGameData(null);
  }

  public void load(final @Nullable GameData data) {
    setGameData(data);
  }

  public void load(final GameChooserEntry entry) {
    setGameData(entry.getGameData());
    ClientSetting.defaultGameName.setValue(entry.getGameData().getGameName());
    ClientSetting.defaultGameUri.setValue(entry.getUri().toString());
    ClientSetting.flush();
  }

  /**
   * Loads game data by parsing a given file.
   *
   * @return True if file parsing was successful and an internal {@code GameData} was set. Otherwise
   *     returns false and internal {@code GameData} is null.
   */
  public boolean load(final File file) {
    Preconditions.checkArgument(
        file.exists(), "Error, file does not exist: " + file.getAbsolutePath());

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
          newData = GameLoader.loadGame(is);
        }
      }
      setGameData(newData);
      return true;
    } catch (final GameParseException e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return false;

    } catch (final EngineVersionException e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return false;

    } catch(final IOException e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return false;
    }
  }

  void setCanSelect(final boolean canSelect) {
    this.canSelect = canSelect;
    notifyObs();
  }

  void setIsHostHeadlessBot(final boolean isHostHeadlessBot) {
    this.hostIsHeadlessBot = isHostHeadlessBot;
    notifyObs();
  }

  /**
   * We don't have a game data (i.e. we are a remote player and the data has not been sent yet), but
   * we still want to display game info.
   */
  void clearDataButKeepGameInfo(
      final String gameName, final String gameRound, final String gameVersion) {
    synchronized (this) {
      gameData = null;
      this.gameName = gameName;
      this.gameRound = gameRound;
      this.gameVersion = gameVersion;
    }
    notifyObs();
  }

  void setGameData(final GameData data) {
    synchronized (this) {
      if (data == null) {
        gameName = gameRound = gameVersion = "-";
      } else {
        gameName = data.getGameName();
        gameRound = "" + data.getSequence().getRound();
        gameVersion = data.getGameVersion().toString();
      }
      gameData = data;
    }
    notifyObs();
  }

  private void notifyObs() {
    super.setChanged();
    super.notifyObservers(gameData);
    super.clearChanged();
  }

  /** Clears AI game over cache and loads default game in a new thread. */
  public void loadDefaultGameNewThread() {
    // clear out ai cached properties (this ended up being the best place to put it, as we have
    // definitely left a game
    // at this point)
    ProAi.gameOverClearCache();
    new Thread(this::loadDefaultGameSameThread).start();
  }

  /**
   * Runs the load default game logic in same thread. Default game is the one that we loaded on
   * startup.
   */
  public void loadDefaultGameSameThread() {
    final String userPreferredDefaultGameUri = ClientSetting.defaultGameUri.getValue().orElse("");

    // we don't want to load a game file by default that is not within the map folders we can load.
    // (ie: if a previous
    // version of triplea was using running a game within its root folder, we shouldn't open it)
    GameChooserEntry selectedGame;
    final String user = ClientFileSystemHelper.getUserRootFolder().toURI().toString();
    if (!userPreferredDefaultGameUri.isEmpty() && userPreferredDefaultGameUri.contains(user)) {
      // if the user has a preferred URI, then we load it, and don't bother parsing or doing
      // anything with the whole
      // game model list
      try {
        final URI defaultUri = new URI(userPreferredDefaultGameUri);
        selectedGame = GameChooserEntry.newInstance(defaultUri);
      } catch (final Exception e) {
        resetToFactoryDefault();
        selectedGame = selectByName();
        if (selectedGame == null) {
          return;
        }
      }
      if (!selectedGame.isGameDataLoaded()) {
        try {
          selectedGame.fullyParseGameData();
        } catch (final GameParseException e) {
          resetToFactoryDefault();
          loadDefaultGameSameThread();
          return;
        }
      }
    } else {
      resetToFactoryDefault();
      selectedGame = selectByName();
      if (selectedGame == null) {
        return;
      }
    }
    load(selectedGame);
  }

  private static void resetToFactoryDefault() {
    ClientSetting.defaultGameUri.resetValue();
    ClientSetting.flush();
  }

  private static GameChooserEntry selectByName() {
    final String userPreferredDefaultGameName = ClientSetting.defaultGameName.getValueOrThrow();

    final GameChooserModel model = new GameChooserModel();
    GameChooserEntry selectedGame = model.findByName(userPreferredDefaultGameName).orElse(null);

    if (selectedGame == null && !model.isEmpty()) {
      selectedGame = model.get(0);
    }
    if (selectedGame == null) {
      return null;
    }
    if (!selectedGame.isGameDataLoaded()) {
      try {
        selectedGame.fullyParseGameData();
      } catch (final GameParseException e) {
        model.removeEntry(selectedGame);
        resetToFactoryDefault();
        return null;
      }
    }
    return selectedGame;
  }
}
