package org.cc.wos;

/**
 * Created by crco0001 on 8/26/2016.
 */
public enum Tags {

    PT(0),AU(1),BA(2),BE(3),GP(4),AF(5),BF(6),CA(7),TI(8),SO(9),SE(10),BS(11),LA(12),DT(13),CT(14),CY(15),CL(16),SP(17),HO(18),DE(19),ID(20),AB(21),C1(22),RP(23),EM(24),RI(25),OI(26),FU(27),FX(28),CR(29),NR(30),TC(31),Z9(32),U1(33),U2(34),PU(35),PI(36),PA(37),SN(38),EI(39),BN(40),J9(41),JI(42),PD(43),PY(44),VL(45),IS(46),PN(47),SU(48),SI(49),MA(50),BP(51),EP(52),AR(53),DI(54),D2(55),PG(56),WC(57),SC(58),GA(59),UT(60),PM(61);

    private int indice;

    private Tags(int indices) {

        this.indice = indices;
    }

    public int getInt() {

        return indice;
    }
}
