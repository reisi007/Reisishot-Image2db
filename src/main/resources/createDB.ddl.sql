CREATE TABLE Camera (
  id           INTEGER PRIMARY KEY             AUTOINCREMENT,
  manufacturer TEXT NOT NULL,
  model        TEXT NOT NULL,
  cropfactor   REAL NOT NULL                   DEFAULT 1,
  UNIQUE (manufacturer, model)
);
CREATE TABLE Image (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  height      INTEGER NOT NULL,
  width       INTEGER NOT NULL,
  fileName    TEXT    NOT NULL,
  focalLength INTEGER NOT NULL,
  camera      INTEGER REFERENCES Camera (id),
  iso         INTEGER NOT NULL,
  av          REAL    NOT NULL,
  tv          TEXT    NOT NULL,
  tv_real     REAL    NOT NULL,
  lens        TEXT,
  captureDate REAL    NOT NULL,
  UNIQUE (fileName, captureDate)
);