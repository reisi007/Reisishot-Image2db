CREATE TABLE Camera (
  id           INTEGER PRIMARY KEY             AUTOINCREMENT,
  manufacturer TEXT NOT NULL,
  model        TEXT NOT NULL,
  cropfactor   REAL NOT NULL                   DEFAULT 1,
  UNIQUE (manufacturer, model)
);
CREATE TABLE Image (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  fileName TEXT    NOT NULL,
  camera   INTEGER REFERENCES Camera (id),
  iso      INTEGER NOT NULL,
  av       REAL    NOT NULL,
  tv       REAL    NOT NULL,
  lens     TEXT,
  date     INTEGER NOT NULL,
  UNIQUE (fileName, date)
);