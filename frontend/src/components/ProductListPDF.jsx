import { Document, Page, Text, View, StyleSheet, PDFViewer } from '@react-pdf/renderer';

const styles = StyleSheet.create({
  viewer: {
    width: '100%',
    height: '800px',
    border: 'none',
    borderRadius: '8px',
    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
  },
  page: {
    padding: 40,
    backgroundColor: '#ffffff',
  },
  header: {
    marginBottom: 30,
  },
  headerGradient: {
    padding: 20,
    marginBottom: 20,
    borderRadius: 8,
    backgroundColor: '#2563eb',
  },
  title: {
    fontSize: 28,
    color: '#ffffff',
    textAlign: 'center',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 14,
    color: '#ffffff',
    opacity: 0.9,
    textAlign: 'center',
  },
  table: {
    display: 'table',
    width: 'auto',
    marginTop: 20,
    borderRadius: 4,
  },
  tableRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderColor: '#e2e8f0',
    minHeight: 35,
    alignItems: 'center',
  },
  tableHeader: {
    backgroundColor: '#2563eb',
  },
  tableHeaderCell: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#ffffff',
    padding: 8,
    textAlign: 'left',
  },
  tableCell: {
    fontSize: 10,
    padding: 8,
    textAlign: 'left',
    color: '#334155',
  },
  categoryHeader: {
    backgroundColor: '#f1f5f9',
    padding: 10,
    marginTop: 20,
    marginBottom: 10,
    borderRadius: 4,
  },
  categoryTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#1e293b',
  },
  summary: {
    marginTop: 30,
    padding: 15,
    backgroundColor: '#f8fafc',
    borderRadius: 8,
  },
  summaryTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#1e293b',
  },
  summaryText: {
    fontSize: 11,
    color: '#475569',
    marginBottom: 5,
  },
  footer: {
    position: 'absolute',
    bottom: 30,
    left: 40,
    right: 40,
    textAlign: 'center',
    color: '#94a3b8',
    fontSize: 10,
    paddingTop: 10,
    borderTopWidth: 1,
    borderColor: '#e2e8f0',
  },
  pageNumber: {
    position: 'absolute',
    bottom: 30,
    right: 40,
    fontSize: 10,
    color: '#94a3b8',
  },
});

const ProductListPDF = ({ products, categories }) => {
  // Group products by category
  const groupedProducts = products.reduce((acc, product) => {
    const categoryId = product.category?.id || 'uncategorized';
    if (!acc[categoryId]) {
      acc[categoryId] = [];
    }
    acc[categoryId].push(product);
    return acc;
  }, {});

  // Calculate statistics
  const stats = {
    total: products.length,
    lowStock: products.filter(p => p.quantitetheo <= 10).length,
    categories: Object.entries(groupedProducts).map(([catId, prods]) => ({
      name: categories.find(c => c.id.toString() === catId)?.name || 'Sans catégorie',
      count: prods.length
    }))
  };

  return (
    <PDFViewer style={styles.viewer}>
      <Document>
        <Page size="A4" style={styles.page}>
          <View style={styles.header}>
            <View style={styles.headerGradient}>
              <Text style={styles.title}>Liste des Produits</Text>
              <Text style={styles.subtitle}>
                {`État du stock au ${new Date().toLocaleDateString()}`}
              </Text>
            </View>
          </View>

          <View style={styles.table}>
            <View style={[styles.tableRow, styles.tableHeader]}>
              <Text style={[styles.tableHeaderCell, { flex: 2 }]}>Nom</Text>
              <Text style={[styles.tableHeaderCell, { flex: 2 }]}>Description</Text>
              <Text style={[styles.tableHeaderCell, { flex: 1 }]}>Code Barre</Text>
              <Text style={[styles.tableHeaderCell, { flex: 1 }]}>Référence</Text>
              <Text style={[styles.tableHeaderCell, { flex: 1 }]}>Prix</Text>
              <Text style={[styles.tableHeaderCell, { flex: 1 }]}>Quantité</Text>
              <Text style={[styles.tableHeaderCell, { flex: 1 }]}>Catégorie</Text>
            </View>

            {products.map((product) => (
              <View key={product.id} style={styles.tableRow}>
                <Text style={[styles.tableCell, { flex: 2 }]}>{product.nom}</Text>
                <Text style={[styles.tableCell, { flex: 2 }]}>{product.description}</Text>
                <Text style={[styles.tableCell, { flex: 1 }]}>{product.codeBarre}</Text>
                <Text style={[styles.tableCell, { flex: 1 }]}>{product.reference}</Text>
                <Text style={[styles.tableCell, { flex: 1 }]}>{product.prix} €</Text>
                <Text style={[styles.tableCell, { flex: 1 }]}>{product.quantitetheo}</Text>
                <Text style={[styles.tableCell, { flex: 1 }]}>{product.category?.name}</Text>
              </View>
            ))}
          </View>

          <View style={styles.summary} break>
            <Text style={styles.summaryTitle}>Résumé du Stock</Text>
            <Text style={styles.summaryText}>{`Total des produits: ${stats.total}`}</Text>
            <Text style={styles.summaryText}>{`Produits en stock faible: ${stats.lowStock}`}</Text>
            {stats.categories.map((cat, index) => (
              <Text key={index} style={styles.summaryText}>
                {`${cat.name}: ${cat.count} produits`}
              </Text>
            ))}
          </View>

          <Text style={styles.footer}>
            Document généré automatiquement par le système de gestion de stock
          </Text>
          
          <Text 
            style={styles.pageNumber} 
            render={({ pageNumber, totalPages }) => (
              `Page ${pageNumber} sur ${totalPages}`
            )} 
            fixed 
          />
        </Page>
      </Document>
    </PDFViewer>
  );
};

export default ProductListPDF;