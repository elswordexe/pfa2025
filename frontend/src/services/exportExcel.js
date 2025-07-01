import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

export function exportProductsToExcel(products, filename = 'produits.xlsx') {
  const data = products.map(p => ({
    Nom: p.nom,
    Description: p.description,
    'Code Barre': p.codeBarre,
    Référence: p.reference,
    Prix: p.prix,
    Quantité: p.quantitetheo,
    Catégorie: p.category?.name,
    'Sous-catégorie': p.subCategory?.name,
  }));
  const worksheet = XLSX.utils.json_to_sheet(data);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Produits');
  const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
  saveAs(new Blob([excelBuffer], { type: 'application/octet-stream' }), filename);
}