package com.bikebuka.bikebuka.ui.view

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import com.androidstudy.daraja.Daraja
import com.androidstudy.daraja.DarajaListener
import com.androidstudy.daraja.model.AccessToken
import com.androidstudy.daraja.model.LNMExpress
import com.androidstudy.daraja.model.LNMResult
import com.androidstudy.daraja.util.TransactionType
import com.bikebuka.bikebuka.R
import com.bikebuka.bikebuka.utils.Constants
import com.bikebuka.bikebuka.utils.Constants.Companion.ACCOUNT_DESC
import com.bikebuka.bikebuka.utils.Constants.Companion.ACCOUNT_REF
import com.bikebuka.bikebuka.utils.Constants.Companion.BASEURL
import com.bikebuka.bikebuka.utils.Constants.Companion.CALLBACK_URL
import com.bikebuka.bikebuka.utils.Constants.Companion.CONSUMER_KEY
import com.bikebuka.bikebuka.utils.Constants.Companion.CONSUMER_SECRET
import com.bikebuka.bikebuka.utils.Constants.Companion.PASSKEY
import com.bikebuka.bikebuka.utils.Constants.Companion.PAYBILL
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import timber.log.Timber
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class BottomSheetDialog : BottomSheetDialogFragment() {
    private lateinit var pickup_date_time: TextInputEditText
    private lateinit var return_date_time: TextInputEditText
    private lateinit var totalPayment: TextView
    private lateinit var btnBook: Button
    private lateinit var phone: TextInputEditText
    private lateinit var daraja: Daraja
    private lateinit var ACCESS_TOKEN: String
    var date: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pickup_date_time = view.findViewById(R.id.pickupDate)
        return_date_time = view.findViewById(R.id.returnDate)
        totalPayment = view.findViewById(R.id.totalPayment)
        btnBook = view.findViewById(R.id.btn_book)
        phone = view.findViewById(R.id.phone)
        initDaraja()
        return_date_time.setOnClickListener {
            showDateTimePicker(return_date_time)
        }
        pickup_date_time.setOnClickListener {
            showDateTimePicker(pickup_date_time)
        }
        btnBook.setOnClickListener {
            pay()
        }
    }

    private fun initDaraja() {
        daraja = Daraja.with(
            CONSUMER_KEY,
            CONSUMER_SECRET,
            object : DarajaListener<AccessToken> {
                override fun onResult(result: AccessToken) {
                    ACCESS_TOKEN = result.access_token
                }

                override fun onError(error: String?) {
                    Timber.d(javaClass.simpleName, "Access Token error: $error")
                }
            })
    }

    @SuppressLint("SetTextI18n")
    private fun pay() {
        if (pickup_date_time.text.isNullOrEmpty() && return_date_time.text.isNullOrEmpty() && phone.text.isNullOrEmpty()) {
            //set price
            Toast.makeText(
                requireContext(),
                "Please input pickup are return dates",
                Toast.LENGTH_LONG
            ).show()

        } else {
            val total = getTotalMinutes(
                pickup_date_time.text.toString(),
                return_date_time.text.toString()
            )
            Timber.d("Minutes: $total")
            totalPayment.text =
                "Total Payment: Ksh ${TimeUnit.MILLISECONDS.toMinutes(total!!)}"

            val lnmExpress = LNMExpress(
                PAYBILL,
                PASSKEY,
                TransactionType.CustomerPayBillOnline,// TransactionType.CustomerBuyGoodsOnline, // TransactionType.CustomerPayBillOnline  <- Apply one of these two
                "1",
                "254" + phone.text.toString().substring(1, phone.text.toString().length),
                PAYBILL,
                "254" + phone.text.toString().substring(1, phone.text.toString().length),
                BASEURL + CALLBACK_URL,
                ACCOUNT_REF,
                ACCOUNT_DESC
            )

            daraja.requestMPESAExpress(lnmExpress, object : DarajaListener<LNMResult> {
                override fun onResult(result: LNMResult) {
                    Toast.makeText(
                        requireContext(),
                        result.ResponseDescription,
                        Toast.LENGTH_LONG
                    ).show()
                    requireContext().startActivity(
                        Intent(
                            requireContext(),
                            HomeActivity::class.java
                        )
                    )
                }

                override fun onError(error: String?) {
                    Toast.makeText(requireContext(), "An error occurred $error", Toast.LENGTH_LONG)
                        .show()
                    Timber.e(error)
                }

            })

        }
    }

    private fun showDateTimePicker(view: TextInputEditText) {
        val currentDate = Calendar.getInstance()
        val myFormat = "MM/dd/yy HH:mm"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        date = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                date?.set(year, monthOfYear, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        date?.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        date?.set(Calendar.MINUTE, minute)
                        Timber.v("The chosen one %s", date?.time)
                        view.setText(sdf.format(date!!.time))
                    }, currentDate[Calendar.HOUR_OF_DAY], currentDate[Calendar.MINUTE], false
                ).show()
            }, currentDate[Calendar.YEAR], currentDate[Calendar.MONTH], currentDate[Calendar.DATE]
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun getTotalMinutes(pickupDate: String, returnDate: String): Long? {
        return (parseDate(pickupDate)?.time?.let { parseDate(returnDate)?.time?.minus(it) })
    }

    private fun parseDate(date: String): Date? {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).parse(date)
    }
}