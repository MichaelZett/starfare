CREATE TABLE user_display_settings (
    player_id varchar(30) NOT NULL REFERENCES user_preferences(player_id) ON DELETE CASCADE,
    setting_key varchar(40) NOT NULL,
    setting_value varchar(512) NOT NULL,
    PRIMARY KEY (player_id, setting_key)
);
