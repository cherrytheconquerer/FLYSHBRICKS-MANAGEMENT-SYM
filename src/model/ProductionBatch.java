package model;

import java.time.LocalDate;

public class ProductionBatch {

    private LocalDate productionDate;
    private int totalBricks;
    private int breakage;
    private double flyashCost;
    private double cementCost;
    private double gypsumCost;
    private double chips6mmCost;
    private double labourCost;
    private double otherCost;
    private double totalCost;
    private double costPerBrick;

    public ProductionBatch(LocalDate productionDate, int totalBricks, int breakage,
                           double flyashCost, double cementCost, double gypsumCost,
                           double chips6mmCost, double labourCost, double otherCost) {
        this.productionDate = productionDate;
        this.totalBricks    = totalBricks;
        this.breakage       = breakage;
        this.flyashCost     = flyashCost;
        this.cementCost     = cementCost;
        this.gypsumCost     = gypsumCost;
        this.chips6mmCost   = chips6mmCost;
        this.labourCost     = labourCost;
        this.otherCost      = otherCost;
        calculate();
    }

    private void calculate() {
        this.totalCost    = flyashCost + cementCost + gypsumCost + chips6mmCost + labourCost + otherCost;
        int good          = totalBricks - breakage;
        this.costPerBrick = good > 0 ? totalCost / good : 0;
    }

    public LocalDate getProductionDate() { return productionDate; }
    public int getTotalBricks()          { return totalBricks; }
    public int getBreakage()             { return breakage; }
    public double getFlyashCost()        { return flyashCost; }
    public double getCementCost()        { return cementCost; }
    public double getGypsumCost()        { return gypsumCost; }
    public double getChips6mmCost()      { return chips6mmCost; }
    public double getLabourCost()        { return labourCost; }
    public double getOtherCost()         { return otherCost; }
    public double getTotalCost()         { return totalCost; }
    public double getCostPerBrick()      { return costPerBrick; }
}
