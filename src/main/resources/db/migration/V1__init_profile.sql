CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL,
    name VARCHAR(255),
    age INT,
    gender VARCHAR(20),
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP
);

CREATE TABLE instruments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE profile_instruments (
    profile_id UUID REFERENCES profile.profiles(id) ON DELETE CASCADE,
    instrument_id INT REFERENCES profile.instruments(id) ON DELETE CASCADE,
    PRIMARY KEY(profile_id, instrument_id)
);