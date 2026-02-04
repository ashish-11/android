package com.paliapp.ecommerce.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.paliapp.ecommerce.data.model.Order
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object BillGenerator {

    fun downloadBill(context: Context, order: Order) {
        val fileName = "Invoice_${order.id.takeLast(6)}_${System.currentTimeMillis()}.pdf"
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()

        val startX = 40f
        var startY = 50f

        // Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 20f
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("WHOLESALE PANDIT MART", (595 / 2).toFloat(), startY, titlePaint)

        startY += 30f
        titlePaint.textSize = 14f
        canvas.drawText("INVOICE", (595 / 2).toFloat(), startY, titlePaint)

        startY += 40f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = sdf.format(Date(order.timestamp))

        canvas.drawText("Order ID: ${order.id}", startX, startY, paint)
        startY += 20f
        canvas.drawText("Date: $date", startX, startY, paint)
        startY += 20f
        canvas.drawText("Customer: ${order.userName}", startX, startY, paint)
        startY += 20f
        canvas.drawText("Mobile: ${order.userMobile}", startX, startY, paint)
        startY += 20f
        canvas.drawText("Address: ${order.address}", startX, startY, paint)

        startY += 40f
        canvas.drawLine(startX, startY, 555f, startY, paint) // Horizontal Line

        startY += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Item", startX, startY, paint)
        canvas.drawText("Qty", 400f, startY, paint)
        canvas.drawText("Price (₹)", 480f, startY, paint)

        startY += 10f
        canvas.drawLine(startX, startY, 555f, startY, paint)

        startY += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        order.items.forEach { item ->
            canvas.drawText(item.name, startX, startY, paint)
            canvas.drawText(item.qty.toString(), 400f, startY, paint)
            canvas.drawText("₹${String.format("%.2f", item.price * item.qty)}", 480f, startY, paint)
            startY += 20f
        }

        startY += 10f
        canvas.drawLine(startX, startY, 555f, startY, paint)

        startY += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Amount:", startX, startY, paint)
        canvas.drawText("₹${String.format("%.2f", order.totalAmount)}", 480f, startY, paint)

        startY += 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Payment Method: ${order.paymentMethod}", startX, startY, paint)
        startY += 20f
        canvas.drawText("Payment Status: ${order.paymentStatus}", startX, startY, paint)
        startY += 20f
        canvas.drawText("Delivery Status: ${order.status}", startX, startY, paint)

        startY += 60f
        titlePaint.textSize = 12f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Thank you for shopping!", (595 / 2).toFloat(), startY, titlePaint)

        pdfDocument.finishPage(page)

        try {
            var savedUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                savedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                savedUri?.let { uri ->
                    val outputStream: OutputStream? = resolver.openOutputStream(uri)
                    outputStream?.use { stream ->
                        pdfDocument.writeTo(stream)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                val outputStream = java.io.FileOutputStream(file)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                savedUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            if (savedUri != null) {
                Toast.makeText(context, "Bill saved to Downloads", Toast.LENGTH_SHORT).show()
                openPdf(context, savedUri)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to download PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer found to open the bill", Toast.LENGTH_LONG).show()
        }
    }
}
