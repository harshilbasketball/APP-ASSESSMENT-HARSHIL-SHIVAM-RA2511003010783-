CREATE DATABASE bankdb;
USE bankdb;

CREATE TABLE accounts (
    account_id INT PRIMARY KEY,
    holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(12, 2) NOT NULL,
    CHECK (balance >= 0)
);

INSERT INTO accounts VALUES
(101, 'HARSHIL', 50000.00),
(102, 'ADITYA', 20000.00);