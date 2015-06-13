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
 * Algoritmo genético desenvolvido para resolver o problema de otimização do Gás Lift
 * Gás Lift é o processo de extrair petróleo de poços com pouca pressão hidrostática. A injeção de gás (vapor de água, gás natual, etc) provoca um aumento da pressão
 * em conjunto com a diminuição da densidade do óleo, permitindo extrair um volume maior de petróleo.
 * A quantidade de petróleo extraído varia em função dos pés cúbicos de gás injetados. Mas à partir de um limitar, a injeção de mais gás provoca o efeito é reverso, temos menos petróleo extraído.
 * O problema é maximizar a quantidade total diária de petróleo extraído, restrito à quantidade de gás diária disponível.
 * 
 * O diferencial deste algoritmo em relação ao estado da arte está no método de reparação, que tenta:
 *   Manter uma quantidade mínima de gás viável em cada poço
 *   Diminuir a quantidade de gás injetado de forma homogênea entre os genes. quando o cromossomo quebrar a restrição.
 *   
 * Outros pontos importantes que foram testados à exaustão são os parâmetros: O conjunto de 1000 indivíduos por população
 * probabilidade de recombinação de 70%, probabilidade de mutação de 10% e 300 iterações apresentaram resultados
 * muito bons, atingindo 3658.9614 BPD Com a seguinte configuração para cada poço:
 * 539.89966, 714.7236, 1312.0532, 881.5734, 1119.4031, 32.11962.
 * 
 * Onde o trabalho de  Buitrago et al. (1996) (De onde a base de poços foi extraída) Chega ao valor de 3629.0 BPD
 * 
 * @author Arthur
 *
 */
public class Ag {
	int TAMANHO_POP = 1000;
	int TAMANHO_CROMOSSOMO = 6;//é o tamanho da base de poços
	float PROB_RECOMBINACAO = 0.7f;
	float PROB_MUTACAO = 0.1f;//é a probabilidade do cromossomo sofrer uma mutação para um valor aleatório dentro do espaço válido
	int QTD_GERACOES = 300;
	
	Random rand = new Random();
	List<Poco> basePocos;
	AtomicInteger geracoesExecutadas;
	
	//Relacionado ao problema
	float maxMSCFd = 4600;//taxa máxima de MSCF/d (centenas de pés cúbicos por dia de gás) esse é o total disponível para ser dividido entre os N poços
	
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
		 * O cromossomo é um array de N números reais representando os metros cúbicos de gás inseridos no poço N
		 * A função de avaliação desse cromossomo é o somatório da quantidade de petróleo recuperado dos N poços
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
		sb.append("Tamanho da população: "+ TAMANHO_POP + "\n");
		sb.append("Probabilidade de recombinação: " + PROB_RECOMBINACAO + "\n");
		sb.append("Probabilidade de mutação: " + PROB_MUTACAO + "\n");
		sb.append("Quantidade de gerações: " + QTD_GERACOES+"\n");
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
	 * Cada indivíduo é criado aleatoriamente. Se quebrar a restrição, corrige com reparação
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
			/**pra cada gene do cromossomo, devemos achar o poço correspondente, e qual a sub-função (segmento de reta) aplicável à quantidade do gás*/
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
		float[] pontuacao = new float[TAMANHO_POP];// A pontuação é a quantidade total de petróleo produzida pelo indivíduo
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
	 * Repara o filho se este for inválido para o problema
	 * para cada gene, verifica se o valor dos metros cúbicos de gás está fora da leitura disponível na base de poços
	 * se estiver, altera o valor para a leitura válida mais próxima.
	 * 
	 * Após isso, verifica se a soma total da distribuição está dentro do limite de injeção de gás diário
	 * se não estiver, realiza a segunda reparação, subtraindo progressivamente a quantidade de gás aplicada
	 * @param filho1
	 */
	private void reparar(float[] individuo) {
		float total = 0;
		//Reparação inicial
		for (int j = 0; j < TAMANHO_CROMOSSOMO; j++){
			float metrosCubGas = individuo[j];
			
			Poco pocoJ = basePocos.get(j);
			
			if (pocoJ.x[0] > metrosCubGas)
				individuo[j] = pocoJ.x[0];
			else if (pocoJ.x[pocoJ.numeroLeituras-1] < metrosCubGas)
				individuo[j] = pocoJ.x[pocoJ.numeroLeituras-1];
			total+= individuo[j];
		}
		
		//Segunda reparação
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


	/** Descarta as instâncias idênticas */
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
	 * A mutação é inteligente, altera uma unica posição do cromossomo, dentro
	 * da faixa de valores onde:
	 *  a função do poço permite
	 *  o somatório do gás usado é <= maxMSCF/d
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
	 * Aplica o crossover entre o pai e a mãe em um ponto aleatório N gerando um novo filho.
	 * @param mae
	 * @param pai
	 * @return
	 */
	private  float[] recombinar(float[] pai, float[] mae) {
		int pontoRecombinacao = rand.nextInt(TAMANHO_CROMOSSOMO);
		//Unico ponto de recombinação
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
	 * A lista de pais não deve ser modificada entre duas chamadas da roleta, dentro de uma única geração do algoritmo
	 * Isto serve para garantir que as probabilidades de sorteio sejam respeitadas, que dependem da posição de cada pai e de sua aptidão.
	 * @param pais
	 * @param total
	 * @param pontuacao
	 * @return
	 */
	private  float[] roleta(List<float[]> pais, float total, float[] pontuacao) {
		float posSorteada = rand.nextFloat();
		
		float ultimaPosicao = 0;//Acumulador das posições visitadas da roleta
		int i = 0;
		for (i = 0; i< pais.size(); i++){
			float[] indv = pais.get(i);
			float pcAdequação = pontuacao[i]/total;//Porcentagem de adequação ao problema entre 0 e 1
			if (posSorteada > ultimaPosicao && posSorteada <= (ultimaPosicao+pcAdequação)){
				break;
			}
			ultimaPosicao += pcAdequação;
			
		}
		if (i == pais.size()) i--;//problemas no arredondamento
		return pais.get(i);

	}
}


