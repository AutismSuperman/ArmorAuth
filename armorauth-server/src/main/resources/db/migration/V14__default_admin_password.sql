-- Align the default seed account with the documented quick-start credential.
-- Only the original default hash is changed so customized admin passwords stay intact.
UPDATE `user_info`
SET `password` = '{bcrypt}$2a$10$tzEgeAwC8jD8Lxvz.IYiCeM4JOhXvZ2GD0UeBWWbVf3.hpL./my02'
WHERE `username` = 'admin'
  AND `password` = '{bcrypt}$2a$10$XwsKMmCkYoJbtLMeeA/.qeVQ9jLw/JNQfZMdkL9wCAoPLPmXEdO8a';
