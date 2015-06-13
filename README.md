# GasLift

Algoritmo genético desenvolvido para resolver o problema de otimização do Gás Lift
  Gás Lift é o processo de extrair petróleo de poços com pouca pressão hidrostática. A injeção de gás (vapor de água, gás natual, etc) provoca um aumento da pressão
  em conjunto com a diminuição da densidade do óleo, permitindo extrair um volume maior de petróleo.
  A quantidade de petróleo extraído varia em função dos pés cúbicos de gás injetados. Mas à partir de um limitar, a injeção de mais gás provoca o efeito é reverso, temos menos petróleo extraído.
  O problema é maximizar a quantidade total diária de petróleo extraído, restrito à quantidade de gás diária disponível.
  
  O diferencial deste algoritmo em relação ao estado da arte está no método de reparação, que tenta:
    Manter uma quantidade mínima de gás viável em cada poço
    Diminuir a quantidade de gás injetado de forma homogênea entre os genes. quando o cromossomo quebrar a restrição.
    
  Outros pontos importantes que foram testados à exaustão são os parâmetros: O conjunto de 1000 indivíduos por população
  probabilidade de recombinação de 70%, probabilidade de mutação de 10% e 300 iterações apresentaram resultados
  muito bons, atingindo 3658.9614 BPD Com a seguinte configuração para cada poço:
  539.89966, 714.7236, 1312.0532, 881.5734, 1119.4031, 32.11962.
  
  Onde o trabalho de  Buitrago et al. (1996) (De onde a base de poços foi extraída) Chega ao valor de 3629.0 BPD