-- ================================================
-- FlyAsh Bricks Management System - Database Setup
-- Run this in MySQL Workbench before starting app
-- ================================================

CREATE DATABASE IF NOT EXISTS bricks_management;
USE bricks_management;

-- Customer Table
CREATE TABLE IF NOT EXISTS customer (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    customer_name   VARCHAR(100) NOT NULL,
    contact_number  VARCHAR(15)  NOT NULL
);

-- Production Batch Table
CREATE TABLE IF NOT EXISTS ProductionBatch (
    batch_id        INT AUTO_INCREMENT PRIMARY KEY,
    production_date DATE          NOT NULL,
    total_bricks    INT           NOT NULL,
    breakage        INT           DEFAULT 0,
    flyash_cost     DOUBLE        DEFAULT 0,
    cement_cost     DOUBLE        DEFAULT 0,
    gypsum_cost     DOUBLE        DEFAULT 0,
    chips6mm_cost   DOUBLE        DEFAULT 0,
    labour_cost     DOUBLE        DEFAULT 0,
    other_cost      DOUBLE        DEFAULT 0,
    total_cost      DOUBLE        DEFAULT 0,
    cost_per_brick  DOUBLE        DEFAULT 0
);

-- Stock Table
CREATE TABLE IF NOT EXISTS Stock (
    stock_id          INT AUTO_INCREMENT PRIMARY KEY,
    batch_id          INT    NOT NULL,
    produced_quantity INT    NOT NULL,
    sold_quantity     INT    DEFAULT 0,
    available_stock   INT    NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES ProductionBatch(batch_id)
);

-- Sales Table
CREATE TABLE IF NOT EXISTS Sales (
    sale_id         INT AUTO_INCREMENT PRIMARY KEY,
    sale_date       DATE   NOT NULL,
    customer_id     INT    NOT NULL,
    bricks_sold     INT    NOT NULL,
    price_per_brick DOUBLE NOT NULL,
    total_amount    DOUBLE NOT NULL,
    paid_amount     DOUBLE DEFAULT 0,
    balance_amount  DOUBLE DEFAULT 0,
    production_cost DOUBLE DEFAULT 0,
    profit          DOUBLE DEFAULT 0,
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

-- Sample Data
INSERT INTO customer (customer_name, contact_number) VALUES
('Ravi Kumar',   '9876543210'),
('Sita Devi',    '9123456780'),
('Anand Singh',  '9988776655');

SELECT 'Database setup complete!' AS Status;
