package com.wrgardnersoft.opr.models;

import Jama.Matrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author William Gardner <wrgardnersoft@gmail.com>
 */
public class OPR {

    static public int[] getTeamList(int teamsPerAlliance, int[][][] teamPlaying) {

        // compute list of teams
        Set<Integer> teamSet = new TreeSet<>();

        for (int i = 0; i < teamPlaying.length; i++) { // # of matches
            for (int j = 0; j < teamsPerAlliance; j++) { // 2 for FTC, 3 for FRC
                teamSet.add(teamPlaying[i][0][j]);
                teamSet.add(teamPlaying[i][1][j]);
            }
        }

        int[] teamList = new int[teamSet.size()];
        { // put sorted team list into team return array and team ArrayList for later
            Iterator<Integer> it = teamSet.iterator();
            int i = 0;
            while (it.hasNext()) {
                teamList[i++] = it.next();
            }
        }
        return teamList;
    }

    /**
     * Computes Offensive Power Rating (OPR) using the MMSE method from FRC or FTC match data
     * <p>
     * The MMSE method is always stable (unlike the traditional least-squares OPR calculation)
     * and does a better job of predicting future unknown match scores.
     * As the number of matches at an event gets large, the OPR values computed for the MMSE method
     * converge to those computed using the traditional method.
     * The MMSE parameter is a function of how random the scores are in each game.
     * If an alliance scores virtually the same amount every time they play, the MMSE parameter should
     * be close to 0.
     * If an alliance's score varies substantially from match to match due to randomness in their
     * ability, their opponent's ability, or random game factors, then the MMSE parameter should be
     * larger.
     * For a typical game, an MMSE parameter of 1-3 is recommended.
     * Using an MMSE parameter of exactly 0 causes the computed OPR values to be identical to the
     * traditional OPR values.
     *
     * @param mmse             input MMSE adjustment parameter (0=normal OPR, 1-3 recommended)
     * @param teamList         input Array of team numbers, sorted
     * @param teamsPerAlliance input number of teams per alliance (usually 3 for FRC, 2 for FTC)
     * @param teamPlaying      input 3D Array of team numbers for alliances playing in each match [match#][alliance 0=R, 1=B][0-1=FTC, 0-2=FRC]
     * @param score            input 2D Array of scores for each match (-1 if not scored yet) [match#][alliance 0=R, 1=B]
     * @return opr      Array of OPR values matching the order of teamList, null if failed
     */
    static public double[] computeMMSE(double mmse, int[] teamList, int teamsPerAlliance, int[][][] teamPlaying, int[][] score) {

        ArrayList<Integer> teamAL = new ArrayList<>();
        int numTeams = teamList.length;
        for (int i = 0; i < numTeams; i++) {
            teamAL.add(teamList[i]);
        }

        // count # of scored matches
        int numScoredMatches = 0;
        for (int[] aScore : score) {
            if (aScore[0] >= 0) {
                numScoredMatches++;
            }
        }

        // setup matrices and vectors
        Matrix Ar = new Matrix(numScoredMatches, numTeams);
        Matrix Ab = new Matrix(numScoredMatches, numTeams);
        Matrix Mr = new Matrix(numScoredMatches, 1);
        Matrix Mb = new Matrix(numScoredMatches, 1);

        Matrix Ao = new Matrix(2 * numScoredMatches, numTeams);
        Matrix Mo = new Matrix(2 * numScoredMatches, 1);

        // populate matrices and vectors
        int match = 0;
        double totalScore = 0;
        for (int i = 0; i < score.length; i++) {
            if (score[i][0] >= 0) { // score match

                for (int j = 0; j < teamsPerAlliance; j++) {
                    Ar.set(match, teamAL.indexOf(teamPlaying[i][0][j]), 1.0);
                    Ab.set(match, teamAL.indexOf(teamPlaying[i][1][j]), 1.0);
                }
                Mr.set(match, 0, score[i][0]);
                Mb.set(match, 0, score[i][1]);

                totalScore += score[i][0];
                totalScore += score[i][1];

                match++;
            }
        }
        Ao.setMatrix(0, numScoredMatches - 1, 0, numTeams - 1, Ar);
        Ao.setMatrix(numScoredMatches, 2 * numScoredMatches - 1, 0, numTeams - 1, Ab);


        double meanTeamOffense = totalScore / (numScoredMatches * 2 * teamsPerAlliance); // 2=alliancesPerMatch
        for (int i = 0; i < numScoredMatches; i++) {
            Mr.set(i, 0, Mr.get(i, 0) - 2.0 * meanTeamOffense);
            Mb.set(i, 0, Mb.get(i, 0) - 2.0 * meanTeamOffense);
        }
        Mo.setMatrix(0, numScoredMatches - 1, 0, 0, Mr);
        Mo.setMatrix(numScoredMatches, 2 * numScoredMatches - 1, 0, 0, Mb);


        // compute inverse of match matrix (Ao' Ao + mmse*I)
        Matrix matchMatrixInv;
        try {
            matchMatrixInv = Ao.transpose().times(Ao).plus(Matrix.identity(numTeams, numTeams).times(mmse)).inverse();
        } catch (Exception e) {
            return null; // matrix not invertible
        }

        // compute OPRs
        double[] opr = new double[teamList.length];
        Matrix Oprm = matchMatrixInv.times(Ao.transpose().times(Mo));
        for (int i = 0; i < numTeams; i++) {
            Oprm.set(i, 0, Oprm.get(i, 0) + meanTeamOffense);

            opr[i] = Oprm.get(i, 0);
        }

        return opr;
    }

    /**
     * Computes Offensive Power Rating (OPR) from FRC or FTC match data
     *
     * @param teamList         returned Array of team numbers, sorted
     * @param teamsPerAlliance input number of teams per alliance (usually 3 for FRC, 2 for FTC)
     * @param teamPlaying      input 3D Array of team numbers for alliances playing in each match [match#][alliance 0=R, 1=B][0-1=FTC, 0-2=FRC]
     * @param score            input 2D Array of scores for each match (-1 if not scored yet) [match#][alliance 0=R, 1=B]
     * @return opr      Array of OPR values matching the order of teamList, null if failed
     */
    static public double[] compute(int[] teamList, int teamsPerAlliance, int[][][] teamPlaying, int[][] score) {
        return computeMMSE(0.0, teamList, teamsPerAlliance, teamPlaying, score);
    }
}
