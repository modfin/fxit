

-- name: latest
-- Select the latest rates.
SELECT content from rate
ORDER BY date DESC
LIMIT 1;


-- name: rate-by-date
-- Select the latest rates.
SELECT content from rate
WHERE date <= :date
ORDER BY date DESC
LIMIT 1;


-- name: rates-in-range
-- Selects all rates in the range.
SELECT content from rate
WHERE :minDate <= date AND date <= :maxDate
ORDER BY date
;


-- name: upsert-rate!
-- insert rate.
INSERT OR REPLACE INTO rate (date, content)
VALUES (:date, :content);


-- name: upsert-euro-to-currency!
-- insert rate.
INSERT OR REPLACE INTO euro_to (currency, date, rate)
VALUES (:currency, :date, :rate);

-- name: avg-rates-in-range
-- insert rate.
SELECT currency, avg(rate) agg_rate, 'avg' name FROM euro_to
WHERE :minDate <= date AND date <= :maxDate
GROUP BY currency;

-- name: min-rates-in-range
-- insert rate.
SELECT currency, min(rate) agg_rate, 'min' name  FROM euro_to
WHERE :minDate <= date AND date <= :maxDate
GROUP BY currency;

-- name: max-rates-in-range
-- insert rate.
SELECT currency, max(rate) agg_rate, 'max' name  FROM euro_to
WHERE :minDate <= date AND date <= :maxDate
GROUP BY currency;
