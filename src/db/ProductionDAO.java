package db;

import model.ProductionBatch;
import java.sql.*;

public class ProductionDAO {

    public void insertProductionBatch(ProductionBatch batch) {
        try (Connection conn = DBConnection.getConnection()) {
            int goodBricks = batch.getTotalBricks() - batch.getBreakage();

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ProductionBatch (production_date,total_bricks,breakage," +
                "flyash_cost,cement_cost,gypsum_cost,chips6mm_cost,labour_cost,other_cost,total_cost,cost_per_brick) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);

            ps.setDate(1, java.sql.Date.valueOf(batch.getProductionDate()));
            ps.setInt(2, batch.getTotalBricks());
            ps.setInt(3, batch.getBreakage());
            ps.setDouble(4, batch.getFlyashCost());
            ps.setDouble(5, batch.getCementCost());
            ps.setDouble(6, batch.getGypsumCost());
            ps.setDouble(7, batch.getChips6mmCost());
            ps.setDouble(8, batch.getLabourCost());
            ps.setDouble(9, batch.getOtherCost());
            ps.setDouble(10, batch.getTotalCost());
            ps.setDouble(11, batch.getCostPerBrick());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int batchId = rs.getInt(1);
                PreparedStatement stock = conn.prepareStatement(
                    "INSERT INTO Stock (batch_id,produced_quantity,sold_quantity,available_stock) VALUES (?,?,0,?)");
                stock.setInt(1, batchId);
                stock.setInt(2, goodBricks);
                stock.setInt(3, goodBricks);
                stock.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void insertSale(int customerId, int qty, double price, double paid) {
        try (Connection conn = DBConnection.getConnection()) {
            int remaining = qty;
            double prodCost = 0;

            ResultSet rs = conn.prepareStatement(
                "SELECT s.stock_id, s.available_stock, p.cost_per_brick " +
                "FROM Stock s JOIN ProductionBatch p ON s.batch_id=p.batch_id " +
                "WHERE s.available_stock>0 ORDER BY s.stock_id").executeQuery();

            while (rs.next() && remaining > 0) {
                int avail  = rs.getInt("available_stock");
                double cst = rs.getDouble("cost_per_brick");
                int ded    = Math.min(avail, remaining);
                prodCost  += ded * cst;
                PreparedStatement upd = conn.prepareStatement(
                    "UPDATE Stock SET available_stock=available_stock-?,sold_quantity=sold_quantity+? WHERE stock_id=?");
                upd.setInt(1, ded); upd.setInt(2, ded); upd.setInt(3, rs.getInt("stock_id"));
                upd.executeUpdate();
                remaining -= ded;
            }

            double total   = qty * price;
            double balance = total - paid;
            double profit  = total - prodCost;

            PreparedStatement sale = conn.prepareStatement(
                "INSERT INTO Sales(sale_date,customer_id,bricks_sold,price_per_brick," +
                "total_amount,paid_amount,balance_amount,production_cost,profit) VALUES(?,?,?,?,?,?,?,?,?)");
            sale.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
            sale.setInt(2, customerId);
            sale.setInt(3, qty);
            sale.setDouble(4, price);
            sale.setDouble(5, total);
            sale.setDouble(6, paid);
            sale.setDouble(7, balance);
            sale.setDouble(8, prodCost);
            sale.setDouble(9, profit);
            sale.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updatePayment(int saleId, double amount) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE Sales SET paid_amount=paid_amount+?,balance_amount=balance_amount-? WHERE sale_id=?");
            ps.setDouble(1, amount);
            ps.setDouble(2, amount);
            ps.setInt(3, saleId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int addCustomer(String name, String phone) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customer(customer_name,contact_number) VALUES(?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int findOrCreateCustomer(String name, String phone) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT customer_id FROM customer WHERE customer_name=? AND contact_number=?");
            ps.setString(1, name); ps.setString(2, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("customer_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return addCustomer(name, phone);
    }
}
