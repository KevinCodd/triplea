All TripleA related processes are started as the `triplea` user, so if you ever get an error because of insufficient permissions, make sure all the files are owned by the `triplea` user.


## Lobby

### Installation
https://github.com/triplea-game/lobby/blob/master/install_lobby

### Check status

Check lobby process is running (lobby port 3304 is a command line arg):
`ps -ef | grep 3304`


### Starting and Stopping
```
sudo service triplea-lobby start|stop|status|restart
```
Be advised, restarting the lobby quits all connections to all bots.
Even if the lobby restarts, the bots won't reconnect automatically, they will have to be restarted on their own.

_This command is currently restricted to users with full sudo rights._

## [The Dice server](https://github.com/triplea-game/dice-server)

Installed on the 'warclub' server

### Installation
TODO

### Check status
TODO

### Starting and Stopping

```
sudo service nginx restart
```

## [NodeBB Forums](https://forums.triplea-game.org)
- Runs on the "NJ" linode server
- NodeBB is deployed via git and dependencies are installed using npm. (TODO: add exact commands to deploy NodeBB)

### Installation
TODO

### Check status and Troubleshooting
If we ever run into problems with the forum and it keeps refusing to start, we need to do a couple things:
Before running any of those commands, we need to be in the correct working directory, `/opt/nodebb/` in our case.
We can do that by executing `cd /opt/nodebb/` in the beginning.
Run `./nodebb upgrade`, this should fix problems most of the time.
If it doesn't, make sure enough memory is available and the database is up and running. (`sudo service mongod status`)
If all of this doesn't help, open an issue at the [NodeBB repository](https://github.com/NodeBB/NodeBB) or create a topic in the [NodeBB community forum](https://community.nodebb.org).

#### Log files
Get last 50 nodebb log lines:
`sudo journalctl | grep "nodebb" | tail -n 50`.

NodeBB uses stdout to log everything, stdout is then attached to journalctl.
jornalctl deletes log files after a couple weeks in order to save space.


### Restarting
- When we have an admin account on the forum, we can restart it using the webinterface.
- If this webinterface is not available because of a crash or a bad configuration file, it can be done by hand:

```
sudo service nodebb restart
sudo service nginx restart
```

_Note: Restarting fails sometimes if not enough resources e.g. memory are available. Always check for a successful restart and reasons for failures._

### Updating
Updating is not always the same, depending on the branch policy of NodeBB.
You should preferably execute all of the following commands as the `nodebb` user, or make sure all the files belong to the `nodebb` user after upgrading.
```
# First of all shut down the forum.
sudo service nodebb stop

# Make sure you are in the correct working directory
cd /opt/nodebb/

# Sometimes the release branch is changed. In this case we need to do a checkout:
# new_branch_name refers to the branch name of the current release of the NodeBB repository.
# It can be found here: https://github.com/NodeBB/NodeBB
git fetch --all
git checkout <new_branch_name>

# All of the following commands need to always be executed:

# Pull the latest changes from the remote repository.
git pull
# Install the latest dependencies.
./nodebb upgrade

# Last but definitely not least restart the forum.
sudo service nodebb start
```


## TripleAWarClub Forum (Legacy)
The old tripleawarclub forum runs on the same server as the lobby, it is powered by XOOPS, written in PHP and uses MySQL as Database scheme. Because of more and more issues with XOOPS we decided to move to the NodeBB forum, which is much easier to maintain. To restart the WarClub forum, just restart nginx:
```
sudo service nginx restart
```

## Bots
```
sudo service triplea-bot@<bot_number> start|stop|status|restart
```
_bot_number_ is used to make multiple bots possible. Use unique bot numbers across all servers to avoid confusion, but unique numbers are only required per server.
The current number scheme is simple:
 - 1 _or 01_ stands for server 0 (tripleawarclub.org) bot number 1.
 - 12 stands for server 1 (NJ/forums.triplea-game.org) bot number 2.
 - 29 stands for server 2 (CA) bot number 9.

etc.
Bots currently use up a lot of RAM, probably too much, this is why only a limited amount of bots can be run on a single server.
Typically 4/5 bots run on a single server each.

Every bot needs its own opened port. We recommend ufw as an easy-to-use tool for managing firewall rules.
The default port is `400${BOT_NUMBER}`, e.g. bot 10 uses port 40010.

### Easy restarting
Sometimes we need to restart all bots at the same time. We could of course just restart each bot individually, but programmers are lazy and that's why there is a simpler method.
On our servers this file exists: `/lib/systemd/system/triplea-bot-starter.service`, and contains something along this content (Of course the bot numbers and counts differ).
```
[Unit]
Description=TripleA Bot Starter

[Service]
Type=oneshot
ExecStart=/bin/echo "Starting TripleA-Bots" ;\
/usr/sbin/service triplea-bot@1 start ;\
/usr/sbin/service triplea-bot@2 start ;\
/usr/sbin/service triplea-bot@3 start ;\
/usr/sbin/service triplea-bot@4 start

[Install]
WantedBy=multi-user.target
```
When enabled using the following commands, the bots are automatically started after every server restart.
```
sudo systemctl enable triplea-bot-starter
sudo systemctl daemon-reload
```

If we now want to restart all default bots, we run:
```
sudo service triplea-bot@* stop
sudo service triplea-bot-starter restart
```
This launches all default bots we defined in our starter service file.
Depending on how many maps are loaded, it may take up to 10 minutes until the bots are online.

### Updating

Run (one time):
```
git clone https://github.com/triplea-game/lobby.git
```

The above will create a 'lobby' folder with support scripts, next run:
```
./lobby/install_bot.sh
```



#### Maps
The maps files are located at `/home/triplea/bots/maps/` by default along with a `download_all_maps` script file from the lobby repository.
To update all maps we simply do the following:
```
cd /home/triplea/bots/maps/
./download_all_maps
```
Depending on the internet connection this could take a couple minutes.

### Log files
Currently TripleA creates log files on its own without relying on stdout.
If anything ever goes wrong, or we just want to check the log files for another reason, we need to look inside the logs directory of the installation folder.
The log folder is located at `/home/triplea/bots/logs/` by default.

## Setting up a new server
Our [triplea-game/lobby repository](https://github.com/triplea-game/lobby) has a couple of useful scripts which enable you to setup a new server in almost no time.
Those scripts enable you to deploy your own lobby and/or bots.
Read more about setup steps [there](https://github.com/triplea-game/lobby#lobby).