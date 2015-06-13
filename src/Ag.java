import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Algoritmo gen�tico desenvolvido para resolver o problema de otimiza��o do G�s Lift
 * G�s Lift � o processo de extrair petr�leo de po�os com pouca press�o hidrost�tica. A inje��o de g�s (vapor de �gua, g�s natual, etc) provoca um aumento da press�o
 * em conjunto com a diminui��o da densidade do �leo, permitindo extrair um volume maior de petr�leo.
 * A quantidade de petr�leo extra�do varia em fun��o dos p�s c�bicos de g�s injetados. Mas � partir de um limitar, a inje��o de mais g�s provoca o efeito � reverso, temos menos petr�leo extra�do.
 * O problema � maximizar a quantidade total di�ria de petr�leo extra�do, restrito � quantidade de g�s di�ria dispon�vel.
 * 
 * O diferencial deste algoritmo em rela��o ao estado da arte est� no m�todo de repara��o, que tenta:
 *   Manter uma quantidade m�nima de g�s vi�vel em cada po�o
 *   Diminuir a quantidade de g�s injetado de forma homog�nea entre os genes. quando o cromossomo quebrar a restri��o.
 *   
 * Outros pontos importantes que foram testados � exaust�o s�o os par�metros: O conjunto de 1000 indiv�duos por popula��o
 * probabilidade de recombina��o de 70%, probabilidade de muta��o de 10% e 300 itera��es apresentaram resultados
 * muito bons, atingindo 3658.9614 BPD Com a seguinte configura��o para cada po�o:
 * 539.89966, 714.7236, 1312.0532, 881.5734, 1119.4031, 32.11962.
 * 
 * Onde o trabalho de  Buitrago et al. (1996) (De onde a base de po�os foi extra�da) Chega ao valor de 3629.0 BPD
 * 
 * @author Arthur
 *
 */
public class Ag {
	int TAMANHO_POP = 1000;
	int TAMANHO_CROMOSSOMO = 6;//� o tamanho da base de po�os
	float PROB_RECOMBINACAO = 0.7f;
	float PROB_MUTACAO = 0.1f;//� a probabilidade do cromossomo sofrer uma muta��o para um valor aleat�rio dentro do espa�o v�lido
	int QTD_GERACOES = 300;
	
	Random rand = new Random();
	List<Poco> basePocos;
	AtomicInteger geracoesExecutadas;
	
	//Relacionado ao problema
	float maxMSCFd = 4600;//taxa m�xima de MSCF/d (centenas de p�s c�bicos por dia de g�s) esse � o total dispon�vel para ser dividido entre os N po�os
	
	class Poco{
		public int numeroLeituras;
		public float[] x;
		public float[] y;
		public Poco(int n, float[] x_, float[] y_){
			numeroLeituras = n;
			x = x_;
			y = y_;
		}
	}
	
	public void run() {
		
		basePocos = new ArrayList<Poco>();
		configurarBasePocos(basePocos);
		
		/**
		 * O cromossomo � um array de N n�meros reais representando os metros c�bicos de g�s inseridos no po�o N
		 * A fun��o de avalia��o desse cromossomo � o somat�rio da quantidade de petr�leo recuperado dos N po�os
		 */
		List<float[]> populacao1 = new ArrayList<float[]>();
		List<float[]> populacao2 = new ArrayList<float[]>();
		List<List<float[]>> populacoes = new ArrayList<List<float[]>>();
		
		populacoes.add(populacao1);
		populacoes.add(populacao2);
		
		int pAtual = 0;
		iniciarPopulacao(populacoes.get(0));
		
		geracoesExecutadas = new AtomicInteger(0);
		
		while (!regraParada()){
			gerarFilhos(populacoes.get(pAtual), populacoes.get(pAtual==1? 0:1));
			pAtual = pAtual==1? 0:1;
		}
		
		mostrarPopulacao(populacoes.get(pAtual));
		return;
		
	}
	
	private void configurarBasePocos(List<Poco> basePocos) {
		/** Dados retirados de Global Optimization Techniques in Gas Allocation for Continuous Flow Gas Lift Systems S. Buitrago, and E. Rodriguez, and D. Espin, Intevep S.A*/
		basePocos.add(new Poco(9, new float[]{0.0f, 32.1f, 93.2f, 186.7f, 316.3f, 490.2f, 721.1f, 1013.4f, 1371.8f},
				new float[]{144.9f, 216.2f, 273.4f, 316.1f, 345.9f, 367.5f, 383.9f, 392.1f, 392.0f}));
		basePocos.add(new Poco(9, new float[]{113.2f, 204.1f, 325.2f, 479.2f, 671.0f, 902.9f, 1183.1f, 1530.7f, 1926.4f},
				new float[]{427.7f, 546.2f, 638.3f, 703.2f, 746.0f, 767.8f, 775.2f, 776.9f, 760.4f}));
		basePocos.add(new Poco(9, new float[]{157.0f, 288.5f, 471.9f, 708.0f, 1003.6f, 1350.0f, 1789.5f, 2319.6f, 2943.2f},
				new float[]{588.6f, 772.0f, 926.4f, 1039.0f, 1115.8f, 1148.1f, 1172.5f, 1177.2f, 1161.8f}));
		basePocos.add(new Poco(8, new float[]{141.5f, 247.9f, 393.9f, 589.9f, 832.5f, 1139.3f, 1527.6f, 1983.0f},
				new float[]{353.8f, 446.8f, 523.0f, 585.6f, 624.4f, 650.7f, 668.3f, 667.4f}));
		basePocos.add(new Poco(11, new float[]{32.1f, 116.1f, 243.4f, 446.8f, 742.6f, 1143.2f, 1667.3f, 2364.8f, 3253.6f, 4397.9f, 5844.4f},
				new float[]{160.5f, 348.1f, 476.0f, 597.0f, 697.4f, 768.9f, 813.6f, 844.7f, 856.2f, 856.6f, 845.4f}));
		basePocos.add(new Poco(11, new float[]{32.1f, 116.1f, 243.4f, 446.8f, 742.6f, 1143.2f, 1667.3f, 2364.8f, 3253.6f, 4397.9f, 5844.4f},
				new float[]{0f, 0f, 0f, 0f, 0f, 97.4f, 168.9f, 213.6f, 244.7f, 301.2f, 295.1f}));
		
	}

	private  void mostrarPopulacao(List<float[]> list) {
		StringBuffer sb = new StringBuffer();
		sb.append("################################################\n");
		sb.append("Tamanho da popula��o: "+ TAMANHO_POP + "\n");
		sb.append("Probabilidade de recombina��o: " + PROB_RECOMBINACAO + "\n");
		sb.append("Probabilidade de muta��o: " + PROB_MUTACAO + "\n");
		sb.append("Quantidade de gera��es: " + QTD_GERACOES+"\n");
		sb.append("################################################\n\n");
		SortedMap<Float, float[]> sm = new TreeMap<Float, float[]>();
		for (float[] indv : list){
			Float score = avaliarIndividuo(indv);
			sm.put(score, indv);
		}
		Set s = sm.keySet();
		Iterator it = s.iterator();
		while (it.hasNext()){
			float v = (Float) it.next();
			sb.append(v);
			sb.append(", ");
			float[] ls = sm.get(v);
			for (float x : ls){
				sb.append(x);
				sb.append(", ");
			}
			sb.append("\n");
			
		}
		
		BufferedWriter out;
		try {
			Date d = new Date();
			out = new BufferedWriter(new FileWriter("output.txt"+d.toString()));
			out.write(sb.toString());
		    out.close();
		} catch (IOException e) {
		}
       
		
	}

	/**
	 * Cada indiv�duo � criado aleatoriamente. Se quebrar a restri��o, corrige com repara��o
	 * @param populacao
	 */
	private  void iniciarPopulacao(List<float[]> populacao){
		
		for (int i = 0; i < TAMANHO_POP; i++){

			float[] individuo = new float[TAMANHO_CROMOSSOMO];
			for (int j = 0; j < TAMANHO_CROMOSSOMO; j++){
				Poco pocoN = basePocos.get(j);
				individuo[j] = (float) rand.nextFloat() * Math.min(pocoN.x[pocoN.numeroLeituras-1], maxMSCFd);
			}
			reparar(individuo);
			populacao.add(individuo);
		}
	}
	
	/**
	 * True se deve parar agora
	 * @param populacao
	 * @return
	 */
	private  boolean regraParada(){
		return geracoesExecutadas.getAndAdd(1) > QTD_GERACOES;
	}
	
	private float avaliarIndividuo(float[] individuo){
		float producaoTotal = 0;
		
		for (int j = 0; j < TAMANHO_CROMOSSOMO; j++){
			float metrosCubGas = individuo[j];
			/**pra cada gene do cromossomo, devemos achar o po�o correspondente, e qual a sub-fun��o (segmento de reta) aplic�vel � quantidade do g�s*/
			Poco pocoJ = basePocos.get(j);
			if (metrosCubGas < pocoJ.x[0])
				continue;
			int k = 0;
			for (k = 0; k < pocoJ.numeroLeituras - 2; k++){
				if (metrosCubGas >= pocoJ.x[k] && metrosCubGas < pocoJ.x[k+1])
					break;//k agora tem o indice do segmento de reta desejado
			}
			
			float x1 = pocoJ.x[k];
			float y1 = pocoJ.y[k];
			float x2 = pocoJ.x[k+1];
			float y2 = pocoJ.y[k+1];
			
			float a = (y2-y1) / (x2-x1);//Coeficiente angular da reta
			float b = y1 - a*x1;//Coeficiente linear
			float producao = a*metrosCubGas + b;
			
			producaoTotal+= producao;

		}
		return producaoTotal;
	}
	
	
	private void gerarFilhos(List<float[]> pais, List<float[]> filhos){
		filhos.clear();
		float[] pontuacao = new float[TAMANHO_POP];// A pontua��o � a quantidade total de petr�leo produzida pelo indiv�duo
		float total = 0;
		
		for (int i = 0; i < TAMANHO_POP; i++){
			pontuacao[i] = avaliarIndividuo(pais.get(i));
			total+= pontuacao[i];
		}
		
		while (filhos.size() < TAMANHO_POP){
			float[] pai = roleta(pais, total, pontuacao);
			float[] mae = roleta(pais, total, pontuacao);
			
			float recombinar = rand.nextFloat();
			
			if (recombinar <= PROB_RECOMBINACAO){
				float[] filho1 = recombinar(pai, mae);
				aplicarMutacao(filho1);
				reparar(filho1);
				filhos.add(filho1);
				
				float[] filho2 = recombinar(mae, pai);
				aplicarMutacao(filho2);
				reparar(filho2);
				filhos.add(filho2);
			}
			else{
				filhos.add(pai);
				filhos.add(mae);
			}
			descartarDuplicatas(filhos);
		}
		
		if (filhos.size() > TAMANHO_POP){
		      filhos.subList(TAMANHO_POP, filhos.size()).clear();
		}
		
		
	}
	

	/**
	 * Repara o filho se este for inv�lido para o problema
	 * para cada gene, verifica se o valor dos metros c�bicos de g�s est� fora da leitura dispon�vel na base de po�os
	 * se estiver, altera o valor para a leitura v�lida mais pr�xima.
	 * 
	 * Ap�s isso, verifica se a soma total da distribui��o est� dentro do limite de inje��o de g�s di�rio
	 * se n�o estiver, realiza a segunda repara��o, subtraindo progressivamente a quantidade de g�s aplicada
	 * @param filho1
	 */
	private void reparar(float[] individuo) {
		float total = 0;
		//Repara��o inicial
		for (int j = 0; j < TAMANHO_CROMOSSOMO; j++){
			float metrosCubGas = individuo[j];
			
			Poco pocoJ = basePocos.get(j);
			
			if (pocoJ.x[0] > metrosCubGas)
				individuo[j] = pocoJ.x[0];
			else if (pocoJ.x[pocoJ.numeroLeituras-1] < metrosCubGas)
				individuo[j] = pocoJ.x[pocoJ.numeroLeituras-1];
			total+= individuo[j];
		}
		
		//Segunda repara��o
		if (total > maxMSCFd){			
			while (total > maxMSCFd){
				float subtrair = ((total - maxMSCFd) / TAMANHO_CROMOSSOMO) + 0.01f;
				for (int j = 0; j < TAMANHO_CROMOSSOMO; j++){
					Poco pocoJ = basePocos.get(j);
					if (individuo[j] - subtrair > pocoJ.x[0]){
						individuo[j] -= subtrair;
						total -= subtrair;
					}
				}
				
			}
		}
	}


	/** Descarta as inst�ncias id�nticas */
	private  void descartarDuplicatas(List<float[]> filhos) {
		Set<float[]> s = new LinkedHashSet<float[]>(filhos);
		filhos.clear();
		filhos.addAll(s);
		
		/*for (int i = 0; i < filhos.size() - 1; i++){
			for (int j = i+1; j < filhos.size(); j++){
				float[] f1 = filhos.get(i);
				float[] f2 = filhos.get(j);
				int k = 0;
				for (k = 0; k < TAMANHO_CROMOSSOMO; k++){
					if (f1[k] != f2[k]) break;
				}
				if (k == TAMANHO_CROMOSSOMO)
					filhos.remove(j);
			}
		}*/
	}

	
	
	
	/** 
	 *
	 * A muta��o � inteligente, altera uma unica posi��o do cromossomo, dentro
	 * da faixa de valores onde:
	 *  a fun��o do po�o permite
	 *  o somat�rio do g�s usado � <= maxMSCF/d
	 * @param indv
	 */
	private void aplicarMutacao(float[] indv) {
		float mutar = rand.nextFloat();
		if (mutar <= PROB_MUTACAO ){
			int pos = rand.nextInt(TAMANHO_CROMOSSOMO);

			Poco pocoJ = basePocos.get(pos);
			float min = pocoJ.x[0];
			
			float restante = maxMSCFd;
			for (int i = 0; i < TAMANHO_CROMOSSOMO; i++){
				if (i == pos) continue;
				restante -= indv[i];
			}
			
			float max = pocoJ.x[pocoJ.numeroLeituras-1];
			
			indv[pos] = min + rand.nextFloat() * (max-min);
		}
	}

	/**
	 * Aplica o crossover entre o pai e a m�e em um ponto aleat�rio N gerando um novo filho.
	 * @param mae
	 * @param pai
	 * @return
	 */
	private  float[] recombinar(float[] pai, float[] mae) {
		int pontoRecombinacao = rand.nextInt(TAMANHO_CROMOSSOMO);
		//Unico ponto de recombina��o
		float[] filho = new float[TAMANHO_CROMOSSOMO];
		
		for (int i = 0; i < TAMANHO_CROMOSSOMO; i++){
			if (i <= pontoRecombinacao)
				filho[i] = pai[i];
			else
				filho[i] = mae[i];
		}
		
		return filho;
	}


	/**
	 * Pontos importantes:
	 * A lista de pais n�o deve ser modificada entre duas chamadas da roleta, dentro de uma �nica gera��o do algoritmo
	 * Isto serve para garantir que as probabilidades de sorteio sejam respeitadas, que dependem da posi��o de cada pai e de sua aptid�o.
	 * @param pais
	 * @param total
	 * @param pontuacao
	 * @return
	 */
	private  float[] roleta(List<float[]> pais, float total, float[] pontuacao) {
		float posSorteada = rand.nextFloat();
		
		float ultimaPosicao = 0;//Acumulador das posi��es visitadas da roleta
		int i = 0;
		for (i = 0; i< pais.size(); i++){
			float[] indv = pais.get(i);
			float pcAdequa��o = pontuacao[i]/total;//Porcentagem de adequa��o ao problema entre 0 e 1
			if (posSorteada > ultimaPosicao && posSorteada <= (ultimaPosicao+pcAdequa��o)){
				break;
			}
			ultimaPosicao += pcAdequa��o;
			
		}
		if (i == pais.size()) i--;//problemas no arredondamento
		return pais.get(i);

	}
}


