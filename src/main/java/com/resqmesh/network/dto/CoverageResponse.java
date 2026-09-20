package com.resqmesh.network.dto;

import java.util.List;

public class CoverageResponse {
    private List<String> band0to4;
    private List<String> band4to8;
    private List<String> band8to15;
    private List<String> band15Plus;
    private List<String> unreachable;
    private int worstBandSize;
    private double weightedCoverageScore;

    public CoverageResponse() {
    }

    public CoverageResponse(List<String> band0to4, List<String> band4to8, List<String> band8to15, List<String> band15Plus, List<String> unreachable, int worstBandSize, double weightedCoverageScore) {
        this.band0to4 = band0to4;
        this.band4to8 = band4to8;
        this.band8to15 = band8to15;
        this.band15Plus = band15Plus;
        this.unreachable = unreachable;
        this.worstBandSize = worstBandSize;
        this.weightedCoverageScore = weightedCoverageScore;
    }

    public List<String> getBand0to4() {
        return band0to4;
    }

    public void setBand0to4(List<String> band0to4) {
        this.band0to4 = band0to4;
    }

    public List<String> getBand4to8() {
        return band4to8;
    }

    public void setBand4to8(List<String> band4to8) {
        this.band4to8 = band4to8;
    }

    public List<String> getBand8to15() {
        return band8to15;
    }

    public void setBand8to15(List<String> band8to15) {
        this.band8to15 = band8to15;
    }

    public List<String> getBand15Plus() {
        return band15Plus;
    }

    public void setBand15Plus(List<String> band15Plus) {
        this.band15Plus = band15Plus;
    }

    public List<String> getUnreachable() {
        return unreachable;
    }

    public void setUnreachable(List<String> unreachable) {
        this.unreachable = unreachable;
    }

    public int getWorstBandSize() {
        return worstBandSize;
    }

    public void setWorstBandSize(int worstBandSize) {
        this.worstBandSize = worstBandSize;
    }

    public double getWeightedCoverageScore() {
        return weightedCoverageScore;
    }

    public void setWeightedCoverageScore(double weightedCoverageScore) {
        this.weightedCoverageScore = weightedCoverageScore;
    }
}
