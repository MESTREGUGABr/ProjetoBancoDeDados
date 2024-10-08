package com.example.oracle_db_project.service;

import com.example.oracle_db_project.dtos.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
public class Querys {

    @PersistenceContext
    private EntityManager entityManager;

    public List<QueryUm> queryUm() {
        String sql = """
                   SELECT
                       np.name AS nome,
                       dp.description AS descricao,
                       p.min_sale_price AS precoMinimo
                   FROM
                       PRODUCT p
                       JOIN NAME_PRODUCT np ON p.product_id = np.product_id AND np.name_language = 'English'
                       JOIN DESCRIPTION_PRODUCT dp ON p.product_id = dp.product_id AND dp.description_language = 'English'
                       JOIN SUPPLIER s ON p.fk_supplier_id = s.supplier_id
                       JOIN STOCK st ON p.product_id = st.product_id
                   WHERE
                       s.supplier_country = 'Japan'
                   GROUP BY
                       np.name, dp.description, p.min_sale_price
                   HAVING
                       SUM(st.quantity) > 20
                   """;

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        List<QueryUm> queryUmList = new ArrayList<>();

        for (Object[] result : results) {
            String nome = (String) result[0];
            String descricao = (String) result[1];
            double precoMinimo = ((Number) result[2]).doubleValue();
            queryUmList.add(new QueryUm(nome, descricao, precoMinimo));
        }
        return queryUmList;
    }

    public List<QueryDois> queryDois() {
        String sql = """
                    SELECT DISTINCT c.name, c.CREDIT_LIMIT, c.CITY, c.STATE
                    FROM
                        CLIENT c
                    WHERE
                        c.COUNTRY = 'United States'
                        AND NOT EXISTS (
                            SELECT *
                            FROM
                                DEMAND d2
                                JOIN ORDER_PRODUCT op ON d2.ORDER_ID = op.ORDER_ID
                                JOIN PRODUCT p ON p.PRODUCT_ID = op.PRODUCT_ID
                                JOIN CATEGORY cat ON cat.CATEGORY_ID = p.FK_CATEGORY_ID
                            WHERE
                                c.CLIENT_ID = d2.CLIENT_ID
                                AND c.NAME = 'Smartphone'
                        )
                        AND NOT EXISTS (
                            SELECT *
                            FROM
                                DEMAND d3
                                JOIN ORDER_PRODUCT op2 ON op2.ORDER_ID = d3.ORDER_ID
                            WHERE
                                c.CLIENT_ID = d3.CLIENT_ID
                            GROUP BY
                                d3.ORDER_ID
                            HAVING
                                SUM(op2.QUANTITY * op2.PRICE) <= 1000
                        )
                        AND c.CLIENT_ID IN (
                            SELECT c1.CLIENT_ID
                            FROM
                                CLIENT c1
                                JOIN DEMAND d ON d.CLIENT_ID = c1.CLIENT_ID
                            GROUP BY
                                c1.CLIENT_ID
                            HAVING
                                COUNT(*) > 4
                        )
                    """;

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        List<QueryDois> queryDoisList = new ArrayList<>();

        for (Object[] result : results) {
            QueryDois queryDois = new QueryDois();
            queryDois.name = (String) result[0];
            queryDois.credit_limit = (BigDecimal) result[1];
            queryDois.city = (String) result[2];
            queryDois.state = (String) result[3];
            queryDoisList.add(queryDois);
        }

        return queryDoisList;
    }

    public List<QueryTres> queryTres() {
        String sql = """
                    SELECT 
                        W.WAREHOUSE_NAME AS Nome,
                        W.WAREHOUSE_STREET || ', ' || 
                        W.WAREHOUSE_NUMBER || ', ' || 
                        W.WAREHOUSE_COMPLEMENT || ', ' || 
                        W.WAREHOUSE_CITY || ', ' || 
                        W.WAREHOUSE_STATE || ', ' || 
                        W.WAREHOUSE_COUNTRY AS Endereco
                    FROM WAREHOUSE W
                    JOIN STOCK S ON W.WAREHOUSE_ID = S.WAREHOUSE_ID
                    WHERE W.WAREHOUSE_ID IN (
                        SELECT W2.WAREHOUSE_ID
                        FROM WAREHOUSE W2
                        JOIN STOCK S2 ON W2.WAREHOUSE_ID = S2.WAREHOUSE_ID
                        WHERE S2.PRODUCT_ID IN (
                            SELECT PRODUCT_ID
                            FROM PRODUCT
                            WHERE FK_CATEGORY_ID = (
                                SELECT CATEGORY_ID
                                FROM CATEGORY
                                WHERE NAME = 'Eletrodomesticos'
                            )
                        )
                        GROUP BY W2.WAREHOUSE_ID
                        HAVING SUM(S2.QUANTITY) >= 20
                    )
                    GROUP BY 
                        W.WAREHOUSE_NAME,
                        W.WAREHOUSE_STREET,
                        W.WAREHOUSE_NUMBER,
                        W.WAREHOUSE_COMPLEMENT,
                        W.WAREHOUSE_CITY,
                        W.WAREHOUSE_STATE,
                        W.WAREHOUSE_COUNTRY
                    HAVING SUM(S.QUANTITY) <= 1000
                """;

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        List<QueryTres> queryTresList = new ArrayList<>();

        for (Object[] result : results) {
            QueryTres queryTres = new QueryTres();
            queryTres.setNome((String) result[0]);
            queryTres.setEndereco((String) result[1]);
            queryTresList.add(queryTres);
        }

        return queryTresList;
    }

    public List<QueryQuatro> queryQuatro() {
        String sql = """
                SELECT
                  SUM(S.QUANTITY) AS Quantidade_Total,
                  AVG(S.QUANTITY) AS Media_Quantidade
                FROM
                  WAREHOUSE W
                JOIN
                  STOCK S ON W.WAREHOUSE_ID = S.WAREHOUSE_ID
                JOIN
                  PRODUCT P ON S.PRODUCT_ID = P.PRODUCT_ID
                JOIN
                  CATEGORY C ON P.FK_CATEGORY_ID = C.CATEGORY_ID
                GROUP BY
                  W.WAREHOUSE_NAME,
                  S.STOCK_ID,
                  C.NAME
                HAVING
                  SUM(S.QUANTITY) >= 60                
                """;

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        List<QueryQuatro> queryQuatroList = new ArrayList<>();

        for (Object[] result : results) {
            QueryQuatro queryQuatro = new QueryQuatro();
            queryQuatro.setQuantidadeTotal((BigDecimal) result[0]);
            queryQuatro.setMediaQuantidade((BigDecimal) result[1]);
            queryQuatroList.add(queryQuatro);
        }

        return queryQuatroList;
    }

    public List<QueryCinco> queryCinco() {
        String sql = """
                SELECT *
                FROM (
                    SELECT C.NAME, C.DESCRIPTION, SUM(OP.QUANTITY * OP.PRICE) AS Faturamento_Total
                    FROM CATEGORY C
                    JOIN PRODUCT P ON P.FK_CATEGORY_ID = C.CATEGORY_ID
                    JOIN ORDER_PRODUCT OP ON OP.ORDER_PRODUCT_ID = P.PRODUCT_ID
                    JOIN DEMAND D ON D.ORDER_ID = OP.ORDER_ID
                    WHERE D.ORDER_DATE > ADD_MONTHS(SYSDATE, -60)
                      AND EXTRACT(MONTH FROM D.ORDER_DATE) > 6
                      AND EXTRACT(MONTH FROM D.ORDER_DATE) <= 12
                    GROUP BY C.NAME, C.DESCRIPTION
                    ORDER BY Faturamento_Total DESC
                )
                WHERE ROWNUM <= 8
                """;

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        List<QueryCinco> queryCincoList = new ArrayList<>();

        for (Object[] result : results) {
            QueryCinco queryCinco = new QueryCinco();
            queryCinco.setNome((String) result[0]);
            queryCinco.setDescricao((String) result[1]);
            queryCinco.setFaturamentoTotal((BigDecimal) result[2]);
            queryCincoList.add(queryCinco);
        }

        return queryCincoList;

    }

    public List<QuerySeis> querySeis() {
        String sql = """
                SELECT COUNT(DISTINCT d.ORDER_ID) AS NUM_PEDIDOS,
                       ROUND((COUNT(DISTINCT d.ORDER_ID) * 100.0 / (SELECT COUNT(*) FROM DEMAND)), 2) AS PORCENTAGEM
                FROM DEMAND d
                JOIN ORDER_PRODUCT op ON op.ORDER_ID = d.ORDER_ID
                JOIN CLIENT c ON c.CLIENT_ID = d.CLIENT_ID
                WHERE d.ORDER_DATE BETWEEN TO_DATE('2023-05-01', 'YYYY-MM-DD') AND TO_DATE('2023-12-31', 'YYYY-MM-DD')
                  AND d.ORDER_MODE = 'In-Store'
                  AND c.COUNTRY = 'United States'
                  AND c.CREDIT_LIMIT > 600
                GROUP BY c.COUNTRY\s
                HAVING SUM(op.QUANTITY * op.PRICE) > 600
                """;
        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        List<QuerySeis> querySeisList = new ArrayList<>();

        for (Object[] result : results) {
            QuerySeis querySeis = new QuerySeis();
            querySeis.setNumPedidos((BigDecimal) result[0]);
            querySeis.setPorcentagem((BigDecimal) result[1]);
            querySeisList.add(querySeis);
        }

        return querySeisList;
    }


}
