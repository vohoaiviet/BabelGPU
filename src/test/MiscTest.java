package test;

import java.util.*;

import deep.DeepNet;
import utils.*;

public class MiscTest
{
	public static void main(String[] args)
	{
		
//			PP.setSep("\n");
//			PP.pMat(CpuUtil.deflatten(randProjMatrix, r, true));
//		PP.p(CpuUtil.randInts(20, 3, 8));
//		for (Path p : FileUtil.listDir("src", true)) PP.p(p);
		
//		FileUtil.move("shit/mover", FileUtil.join("shit6", "sub7"));
//		for (String p : FileUtil.listDir("shit6", "*.txt", true)) PP.p(p);
//		FileUtil.makeDir("kirito");
//		FileUtil.makeTempDir("kirito", "");
//		FileUtil.makeTempFile("kirito", ".txt", "");
		float lastLoss = Float.POSITIVE_INFINITY;
		lastLoss = 400;
		float curLoss = 399.59999f;
		float improvement = (lastLoss - curLoss) / lastLoss;
		boolean decay = Float.isNaN(improvement) || improvement < 0.001f;
		PP.p(Float.POSITIVE_INFINITY > 2);
//		for (String line : FileUtil.iterable("../BabelGPU", "test.sh")) PP.p(line);
		
		PP.p(MiscUtil.splitStrNum("Z_a-3.4"));
		PP.p(MiscUtil.splitStrNum("Z_a"));
		
		Integer[] a = new Integer[] {3, null, 5, 6};
		a = new Integer[] {8, null, null, null, null};
		String[] s = new String[] {"aa", "bbb", "c", "dddd"};
        s = new String[] {null, null, null, null, "dud"};
		Pair<Integer, String>[] p = Pair.unzip(new Pair<>(a, s));
		PP.po(p);
		PP.po(Pair.zip(p));
		
		Integer A[] = new Integer[] {3, 4, 5, 1};
		PP.po(MiscUtil.map(A, new MiscUtil.DualFunc<Integer, String>()
				{
					@Override
					public String apply(Integer obj)
					{
						return obj*2 + "dudulu";
					}
					@Override
					public Class<String> outClass() { return String.class; }
				}));
		
		PP.p("DONE");
	}

}
