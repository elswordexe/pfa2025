import React from 'react';
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
    padding: 30,
    backgroundColor: '#ffffff',
  },
  header: {
    marginBottom: 20,
    textAlign: 'center',
    color: '#1e40af',
  },
  title: {
    fontSize: 24,
    marginBottom: 10,
  },
  subtitle: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 20,
  },
  infoSection: {
    marginBottom: 20,
    padding: 10,
    backgroundColor: '#f8fafc',
    borderRadius: 4,
  },
  zoneSection: {
    marginBottom: 15,
    borderBottom: 1,
    borderBottomColor: '#e5e7eb',
  },
  zoneHeader: {
    backgroundColor: '#2563eb',
    color: '#ffffff',
    padding: 8,
    marginBottom: 10,
    borderRadius: 4,
  },
  table: {
    display: 'table',
    width: 'auto',
    marginBottom: 10,
    borderRadius: 4,
  },
  tableRow: {
    flexDirection: 'row',
    borderBottomColor: '#e5e7eb',
    borderBottomWidth: 1,
    minHeight: 25,
    alignItems: 'center',
  },
  tableHeader: {
    backgroundColor: '#f3f4f6',
  },
  tableCell: {
    padding: 5,
    fontSize: 10,
  },
  productCell: { width: '25%' },
  codeCell: { width: '15%' },
  qtyCell: { width: '12%', textAlign: 'right' },
  statusCell: { width: '12%', textAlign: 'center' },
  footer: {
    position: 'absolute',
    bottom: 30,
    left: 30,
    right: 30,
    textAlign: 'center',
    color: '#9ca3af',
    fontSize: 10,
    paddingTop: 10,
    borderTopWidth: 1,
    borderColor: '#e5e7eb',
  },
});

const InventoryPDF = ({ planId, zones, validatedProducts = [] }) => {
  const validatedMap = {};
  validatedProducts.forEach(vp => {
    const key = `${vp.id}-${vp.validatedZone || vp.zoneId}`;
    validatedMap[key] = vp;
  });

  return (
    <PDFViewer style={styles.viewer}>
      <Document>
        <Page size="A4" style={styles.page}>
          <View style={styles.header}>
            <Text style={styles.title}>Rapport d'Écarts d'Inventaire</Text>
            <Text style={styles.subtitle}>{`Plan #${planId} - ${new Date().toLocaleDateString()}`}</Text>
          </View>

          {zones.map((zone) => (
            <View key={zone.id} style={styles.zoneSection}>
              <View style={styles.zoneHeader}>
                <Text>{`Zone: ${zone.name}`}</Text>
              </View>

              <View style={styles.table}>
                {/* Table Header */}
                <View style={[styles.tableRow, styles.tableHeader]}>
                  <Text style={[styles.tableCell, styles.productCell]}>Produit</Text>
                  <Text style={[styles.tableCell, styles.codeCell]}>Code Barre</Text>
                  <Text style={[styles.tableCell, styles.qtyCell]}>Théorique</Text>
                  <Text style={[styles.tableCell, styles.qtyCell]}>Qté Avant Validation</Text>
                  <Text style={[styles.tableCell, styles.qtyCell]}>Manuel</Text>
                  <Text style={[styles.tableCell, styles.qtyCell]}>Scan</Text>
                  <Text style={[styles.tableCell, styles.qtyCell]}>Écart</Text>
                  <Text style={[styles.tableCell, styles.statusCell]}>État</Text>
                </View>

                {/* Table Body */}
                {zone.zoneProduits?.map((product) => {
                  const key = `${product.id}-${zone.id}`;
                  const validated = validatedMap[key];
                  let status = '';
                  let quantiteValidee = '-';
                  let quantiteTheorique = product.quantiteTheorique || '0';
                  let quantiteAvantValidation = '';
                  let ecart = '-';
                  if (validated) {
                    status = 'Validé';
                    quantiteValidee = validated.quantiteValidee ?? validated.quantite ?? '-';
                    if (validated.quantiteAvantValidation !== undefined) {
                      quantiteAvantValidation = validated.quantiteAvantValidation;
                    } else if (validated.oldQuantiteAvant !== undefined) {
                      quantiteAvantValidation = validated.oldQuantiteAvant;
                    } else {
                      quantiteAvantValidation = '';
                    }
                    if (
                      quantiteValidee !== undefined && quantiteValidee !== '-' &&
                      quantiteAvantValidation !== undefined && quantiteAvantValidation !== '' && quantiteAvantValidation !== '-'
                    ) {
                      ecart = Number(quantiteValidee) - Number(quantiteAvantValidation);
                    }
                    return (
                      <View key={product.id} style={styles.tableRow}>
                        <Text style={[styles.tableCell, styles.productCell]}>
                          {product.nom || '-'}
                        </Text>
                        <Text style={[styles.tableCell, styles.codeCell]}>
                          {product.codeBarre || '-'}
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                          {quantiteAvantValidation}
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                          {quantiteValidee}
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                        </Text>
                        <Text style={[
                          styles.tableCell,
                          styles.qtyCell,
                          ecart !== '-' && !isNaN(ecart)
                            ? (ecart > 0
                                ? { color: '#16a34a', fontWeight: 'bold' }
                                : (ecart < 0
                                    ? { color: '#dc2626', fontWeight: 'bold' } 
                                    : { color: '#1f2937', fontWeight: 'bold' })) 
                            : { color: '#1f2937' }
                        ]}>
                          {ecart}
                        </Text>
                        <Text style={[styles.tableCell, styles.statusCell]}>
                          {status}
                        </Text>
                      </View>
                    );
                  } else {
                    const ecart = (product.quantiteManuelle || product.quantiteScan || 0) - product.quantiteTheorique;
                    status = ecart === 0 ? 'Conforme' : ecart > 0 ? 'Surplus' : 'Manquant';
                    return (
                      <View key={product.id} style={styles.tableRow}>
                        <Text style={[styles.tableCell, styles.productCell]}>
                          {product.nom || '-'}
                        </Text>
                        <Text style={[styles.tableCell, styles.codeCell]}>
                          {product.codeBarre || '-'}
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                          {quantiteTheorique}
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                          {product.quantiteManuelle || '-'}
                        </Text>
                        <Text style={[styles.tableCell, styles.qtyCell]}>
                          {product.quantiteScan || '-'}
                        </Text>
                        <Text style={[
                          styles.tableCell,
                          styles.qtyCell,
                          ecart !== '-' && !isNaN(ecart)
                            ? (ecart > 0
                                ? { color: '#16a34a', fontWeight: 'bold' }
                                : (ecart < 0
                                    ? { color: '#dc2626', fontWeight: 'bold' }
                                    : { color: '#1f2937', fontWeight: 'bold' }))
                            : { color: '#1f2937' }
                        ]}>
                          {ecart}
                        </Text>
                        <Text style={[styles.tableCell, styles.statusCell]}>
                          {status}
                        </Text>
                      </View>
                    );
                  }
                })}
              </View>
            </View>
          ))}

          <View style={styles.footer}>
            <Text>Document généré le {new Date().toLocaleString()}</Text>
          </View>
        </Page>
      </Document>
    </PDFViewer>
  );
};

export default InventoryPDF;