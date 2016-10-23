DROP TABLE IF EXISTS rate;

CREATE TABLE rate (
  date TEXT PRIMARY KEY,
  content TEXT
)
;

CREATE INDEX date_index
ON rate (date DESC);


DROP TABLE IF EXISTS euro_to;

CREATE TABLE euro_to (
  currency TEXT,
  date TEXT,
  rate DOUBLE PRECISION,
  CONSTRAINT pk_euro_to PRIMARY KEY (currency, date)
)
;

CREATE INDEX euro_to_index_date
ON euro_to (date);

CREATE INDEX euro_to_index_currency_date
ON euro_to (currency, date);
