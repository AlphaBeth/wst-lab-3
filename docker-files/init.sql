CREATE TABLE exterminatus
(
  id        BIGINT PRIMARY KEY,
  initiator VARCHAR(512) NOT NULL,
  reason    VARCHAR(2048),
  method    VARCHAR(255) NOT NULL,
  planet    VARCHAR(255) NOT NULL,
  date      TIMESTAMP    NOT NULL
);

CREATE SEQUENCE exterminatus_sequence START WITH 5;

INSERT INTO exterminatus (id, initiator, reason, method, planet, date)
VALUES (1, 'Хорус Луперкаль', 'Начало Ереси', 'вирусная бомбадировка', 'Истваан III', '30555-11-11 00:00:00.000000');
INSERT INTO exterminatus (id, initiator, reason, method, planet, date)
VALUES (2, 'Инквизитор Криптман', 'Нашествие тиранидов', 'вирусная бомбадировка', 'Тетрис',
        '40000-11-11 00:00:00.000000');
INSERT INTO exterminatus (id, initiator, reason, method, planet, date)
VALUES (3, 'Инквизитор Криптман', 'Нашествие тиранидов', 'вирусная бомбадировка', 'Кроделис, сегментум Ультима',
        '40000-12-11 00:00:00.000000');
INSERT INTO exterminatus (id, initiator, reason, method, planet, date)
VALUES (4, 'Инквизитор Адрастиа', 'Нашествие тиранидов', 'циклонные торпеды', 'Тифон Примарис, сегментум Ультима',
        '30555-11-11 00:00:00.000000');