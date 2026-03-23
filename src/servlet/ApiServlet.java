package servlet;

import db.DBConnection;
import db.ProductionDAO;
import model.ProductionBatch;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;

@WebServlet("/api/*")
public class ApiServlet extends HttpServlet {

    // ── GET requests ─────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json; charset=UTF-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = res.getWriter();
        String path = req.getPathInfo();

        try {
            switch (path) {

                case "/dashboard": {
                    Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT " +
                        "COALESCE((SELECT SUM(available_stock) FROM Stock),0) AS stock," +
                        "COALESCE((SELECT SUM(total_amount)   FROM Sales),0)  AS revenue," +
                        "COALESCE((SELECT SUM(profit)         FROM Sales),0)  AS profit," +
                        "COALESCE((SELECT SUM(balance_amount) FROM Sales),0)  AS pending");
                    ResultSet rs = ps.executeQuery();
                    int stock=0; double revenue=0,profit=0,pending=0;
                    if (rs.next()) {
                        stock=rs.getInt("stock"); revenue=rs.getDouble("revenue");
                        profit=rs.getDouble("profit"); pending=rs.getDouble("pending");
                    }
                    PreparedStatement pt = conn.prepareStatement(
                        "SELECT s.sale_id,c.customer_name,s.bricks_sold,s.price_per_brick," +
                        "s.total_amount,s.paid_amount,s.balance_amount,s.sale_date " +
                        "FROM Sales s JOIN customer c ON s.customer_id=c.customer_id " +
                        "ORDER BY s.sale_date DESC LIMIT 5");
                    ResultSet rt = pt.executeQuery();
                    StringBuilder txns = new StringBuilder("[");
                    boolean first = true;
                    while (rt.next()) {
                        if (!first) txns.append(",");
                        first = false;
                        double bal = rt.getDouble("balance_amount");
                        double pd  = rt.getDouble("paid_amount");
                        String status = bal<=0?"Paid":pd>0?"Partial":"Pending";
                        txns.append(String.format(
                            "{\"id\":%d,\"customer\":\"%s\",\"qty\":%d,\"price\":%.2f," +
                            "\"total\":%.2f,\"paid\":%.2f,\"balance\":%.2f,\"date\":\"%s\",\"status\":\"%s\"}",
                            rt.getInt("sale_id"), rt.getString("customer_name").replace("\"","\\\""),
                            rt.getInt("bricks_sold"), rt.getDouble("price_per_brick"),
                            rt.getDouble("total_amount"), pd, bal,
                            rt.getDate("sale_date"), status));
                    }
                    txns.append("]");
                    out.printf("{\"stock\":%d,\"revenue\":%.2f,\"profit\":%.2f,\"pending\":%.2f,\"transactions\":%s}",
                        stock, revenue, profit, pending, txns);
                    conn.close();
                    break;
                }

                case "/customers": {
                    Connection conn = DBConnection.getConnection();
                    ResultSet rs = conn.prepareStatement(
                        "SELECT customer_id,customer_name,contact_number FROM customer ORDER BY customer_name")
                        .executeQuery();
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"phone\":\"%s\"}",
                            rs.getInt("customer_id"),
                            rs.getString("customer_name").replace("\"","\\\""),
                            rs.getString("contact_number")));
                        first = false;
                    }
                    out.print(sb.append("]").toString());
                    conn.close();
                    break;
                }

                case "/pending": {
                    Connection conn = DBConnection.getConnection();
                    ResultSet rs = conn.prepareStatement(
                        "SELECT s.sale_id,c.customer_name,s.bricks_sold,s.total_amount," +
                        "s.paid_amount,s.balance_amount,s.sale_date " +
                        "FROM Sales s JOIN customer c ON s.customer_id=c.customer_id " +
                        "WHERE s.balance_amount>0 ORDER BY s.sale_date DESC").executeQuery();
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append(String.format(
                            "{\"id\":%d,\"customer\":\"%s\",\"qty\":%d,\"total\":%.2f,\"paid\":%.2f,\"balance\":%.2f,\"date\":\"%s\"}",
                            rs.getInt("sale_id"), rs.getString("customer_name").replace("\"","\\\""),
                            rs.getInt("bricks_sold"), rs.getDouble("total_amount"),
                            rs.getDouble("paid_amount"), rs.getDouble("balance_amount"),
                            rs.getDate("sale_date")));
                        first = false;
                    }
                    out.print(sb.append("]").toString());
                    conn.close();
                    break;
                }

                case "/statement": {
                    int custId = Integer.parseInt(req.getParameter("id"));
                    Connection conn = DBConnection.getConnection();
                    PreparedStatement psSum = conn.prepareStatement(
                        "SELECT c.customer_name,c.contact_number," +
                        "SUM(s.total_amount) AS total,SUM(s.paid_amount) AS paid,SUM(s.balance_amount) AS balance " +
                        "FROM Sales s JOIN customer c ON s.customer_id=c.customer_id " +
                        "WHERE s.customer_id=? GROUP BY c.customer_name,c.contact_number");
                    psSum.setInt(1, custId);
                    ResultSet rsSum = psSum.executeQuery();
                    if (!rsSum.next()) { out.print("{\"error\":\"Not found\"}"); break; }

                    PreparedStatement psTxn = conn.prepareStatement(
                        "SELECT sale_id,sale_date,bricks_sold,price_per_brick,total_amount,paid_amount,balance_amount " +
                        "FROM Sales WHERE customer_id=? ORDER BY sale_date DESC");
                    psTxn.setInt(1, custId);
                    ResultSet rsTxn = psTxn.executeQuery();

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("{\"name\":\"%s\",\"phone\":\"%s\",\"total\":%.2f,\"paid\":%.2f,\"balance\":%.2f,\"txns\":[",
                        rsSum.getString("customer_name").replace("\"","\\\""),
                        rsSum.getString("contact_number"),
                        rsSum.getDouble("total"), rsSum.getDouble("paid"), rsSum.getDouble("balance")));
                    boolean first = true;
                    while (rsTxn.next()) {
                        if (!first) sb.append(",");
                        double bal = rsTxn.getDouble("balance_amount");
                        double pd  = rsTxn.getDouble("paid_amount");
                        sb.append(String.format(
                            "{\"id\":%d,\"date\":\"%s\",\"qty\":%d,\"price\":%.2f,\"total\":%.2f,\"paid\":%.2f,\"balance\":%.2f}",
                            rsTxn.getInt("sale_id"), rsTxn.getDate("sale_date"),
                            rsTxn.getInt("bricks_sold"), rsTxn.getDouble("price_per_brick"),
                            rsTxn.getDouble("total_amount"), pd, bal));
                        first = false;
                    }
                    sb.append("]}");
                    out.print(sb.toString());
                    conn.close();
                    break;
                }

                case "/reports/monthly": {
                    String month = req.getParameter("month");
                    Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) AS txns,SUM(bricks_sold) AS bricks," +
                        "SUM(total_amount) AS revenue,SUM(paid_amount) AS received," +
                        "SUM(balance_amount) AS pending,SUM(profit) AS profit " +
                        "FROM Sales WHERE DATE_FORMAT(sale_date,'%Y-%m')=?");
                    ps.setString(1, month);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        out.printf("{\"txns\":%d,\"bricks\":%d,\"revenue\":%.2f,\"received\":%.2f,\"pending\":%.2f,\"profit\":%.2f}",
                            rs.getInt("txns"), rs.getInt("bricks"),
                            rs.getDouble("revenue"), rs.getDouble("received"),
                            rs.getDouble("pending"), rs.getDouble("profit"));
                    }
                    conn.close();
                    break;
                }

                case "/reports/datewise": {
                    String from = req.getParameter("from");
                    String to   = req.getParameter("to");
                    Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) AS txns,SUM(bricks_sold) AS bricks," +
                        "SUM(total_amount) AS revenue,SUM(profit) AS profit " +
                        "FROM Sales WHERE sale_date BETWEEN ? AND ?");
                    ps.setDate(1, java.sql.Date.valueOf(from));
                    ps.setDate(2, java.sql.Date.valueOf(to));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        out.printf("{\"txns\":%d,\"bricks\":%d,\"revenue\":%.2f,\"profit\":%.2f}",
                            rs.getInt("txns"), rs.getInt("bricks"),
                            rs.getDouble("revenue"), rs.getDouble("profit"));
                    }
                    conn.close();
                    break;
                }

                case "/reports/stock": {
                    Connection conn = DBConnection.getConnection();
                    ResultSet rs = conn.prepareStatement(
                        "SELECT COALESCE(SUM(available_stock),0) AS stock FROM Stock").executeQuery();
                    int stock = rs.next() ? rs.getInt("stock") : 0;
                    out.printf("{\"stock\":%d,\"low\":%b}", stock, stock < 500);
                    conn.close();
                    break;
                }

                case "/reports/txnhistory": {
                    Connection conn = DBConnection.getConnection();
                    ResultSet rs = conn.prepareStatement(
                        "SELECT s.sale_id,s.sale_date,c.customer_name,s.bricks_sold," +
                        "s.price_per_brick,s.total_amount,s.paid_amount,s.balance_amount " +
                        "FROM Sales s JOIN customer c ON s.customer_id=c.customer_id " +
                        "ORDER BY s.sale_id DESC LIMIT 50").executeQuery();
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        double bal = rs.getDouble("balance_amount");
                        double pd  = rs.getDouble("paid_amount");
                        String status = bal<=0?"Paid":pd>0?"Partial":"Pending";
                        sb.append(String.format(
                            "{\"id\":%d,\"date\":\"%s\",\"customer\":\"%s\",\"qty\":%d," +
                            "\"price\":%.2f,\"total\":%.2f,\"paid\":%.2f,\"balance\":%.2f,\"status\":\"%s\"}",
                            rs.getInt("sale_id"), rs.getDate("sale_date"),
                            rs.getString("customer_name").replace("\"","\\\""),
                            rs.getInt("bricks_sold"), rs.getDouble("price_per_brick"),
                            rs.getDouble("total_amount"), pd, bal, status));
                        first = false;
                    }
                    out.print(sb.append("]").toString());
                    conn.close();
                    break;
                }

                default:
                    out.print("{\"error\":\"Unknown endpoint\"}");
            }
        } catch (Exception e) {
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // ── POST requests ─────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json; charset=UTF-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = res.getWriter();
        String path = req.getPathInfo();

        try {
            switch (path) {

                case "/production": {
                    String date    = req.getParameter("date");
                    int total      = Integer.parseInt(req.getParameter("totalBricks"));
                    int breakage   = Integer.parseInt(req.getParameter("breakage"));
                    double flyash  = Double.parseDouble(req.getParameter("flyash"));
                    double cement  = Double.parseDouble(req.getParameter("cement"));
                    double gypsum  = Double.parseDouble(req.getParameter("gypsum"));
                    double chips   = Double.parseDouble(req.getParameter("chips"));
                    double labour  = Double.parseDouble(req.getParameter("labour"));
                    double other   = Double.parseDouble(req.getParameter("other"));

                    ProductionBatch batch = new ProductionBatch(
                        LocalDate.parse(date), total, breakage,
                        flyash, cement, gypsum, chips, labour, other);
                    new ProductionDAO().insertProductionBatch(batch);

                    int good = total - breakage;
                    double totalCost = flyash+cement+gypsum+chips+labour+other;
                    double cpb = good>0 ? totalCost/good : 0;
                    out.printf("{\"success\":true,\"goodBricks\":%d,\"totalCost\":%.2f,\"costPerBrick\":%.2f}",
                        good, totalCost, cpb);
                    break;
                }

                case "/sale": {
                    String name    = req.getParameter("custName");
                    String phone   = req.getParameter("custPhone");
                    int qty        = Integer.parseInt(req.getParameter("qty"));
                    double price   = Double.parseDouble(req.getParameter("price"));
                    double paid    = Double.parseDouble(req.getParameter("paid"));
                    String date    = req.getParameter("date");

                    // Check stock
                    Connection conn = DBConnection.getConnection();
                    ResultSet rsStock = conn.prepareStatement(
                        "SELECT COALESCE(SUM(available_stock),0) AS stock FROM Stock").executeQuery();
                    int avail = rsStock.next() ? rsStock.getInt("stock") : 0;
                    conn.close();

                    if (qty > avail) {
                        out.printf("{\"success\":false,\"message\":\"Not enough stock! Available: %d\"}", avail);
                        break;
                    }

                    ProductionDAO dao = new ProductionDAO();
                    int custId = dao.findOrCreateCustomer(name, phone);
                    dao.insertSale(custId, qty, price, paid);

                    double total   = qty * price;
                    double balance = total - paid;
                    out.printf("{\"success\":true,\"message\":\"Sale recorded!\",\"total\":%.2f,\"balance\":%.2f}",
                        total, balance);
                    break;
                }

                case "/payment": {
                    int saleId    = Integer.parseInt(req.getParameter("saleId"));
                    double amount = Double.parseDouble(req.getParameter("amount"));

                    Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT balance_amount FROM Sales WHERE sale_id=?");
                    ps.setInt(1, saleId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) { out.print("{\"success\":false,\"message\":\"Sale not found\"}"); break; }
                    double bal = rs.getDouble("balance_amount");
                    conn.close();

                    if (amount > bal) {
                        out.printf("{\"success\":false,\"message\":\"Exceeds balance Rs.%.2f\"}", bal);
                        break;
                    }
                    new ProductionDAO().updatePayment(saleId, amount);
                    out.printf("{\"success\":true,\"message\":\"Payment of Rs.%.2f recorded!\"}", amount);
                    break;
                }

                default:
                    out.print("{\"error\":\"Unknown endpoint\"}");
            }
        } catch (Exception e) {
            out.printf("{\"success\":false,\"message\":\"Error: %s\"}", e.getMessage());
        }
    }
}
