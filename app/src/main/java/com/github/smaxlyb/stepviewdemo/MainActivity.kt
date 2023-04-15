package com.github.smaxlyb.stepviewdemo

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.github.smaxlyb.stepview.StepModel
import com.github.smaxlyb.stepview.StepView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        step.setNodes(
            mutableListOf(
                StepModel("步骤一", "这是步骤一这是步骤一这是步骤一这是步骤一这是步骤一这是步骤一这是步骤一"),
                StepModel("步骤二"),
                StepModel("步骤三步骤三步骤三步骤三步骤三步骤三步骤三步骤三步骤三步骤三步骤三", "这是步骤三这是步骤三这是步骤三这是步骤三这是步骤三这是步骤三这是步骤三"),
                StepModel("步骤四")
            )
        )
            .setOnNodeClickListener { i, ndStepModel ->
                Toast.makeText(this, ndStepModel.title, Toast.LENGTH_SHORT).show()
            }
        initListener()
    }

    private fun initListener() {

        rp4_rd6.setOnClickListener {
            rp4_rd2.isEnabled = false
            rp4_rd3.isEnabled = false
            rp4_rd4.isEnabled = false
            rp4_rd5.isEnabled = false
            rp4_rd6.isEnabled = false
            step.removeNodeAt(3)
        }

        rp5_rd1.setOnClickListener {
            rp6_rd1.isEnabled = true
            rp6_rd2.isEnabled = true
            rp6_rd3.isEnabled = false
            rp6_rd4.isEnabled = false

            step.setOrientation(StepView.Orientation.Horizontal)
        }
        rp5_rd2.setOnClickListener {
            rp6_rd1.isEnabled = false
            rp6_rd2.isEnabled = false
            rp6_rd3.isEnabled = true
            rp6_rd4.isEnabled = true

            step.setOrientation(StepView.Orientation.Vertical)
        }

        listOf<RadioButton>(rp1_rd2, rp2_rd2, rp3_rd2, rp4_rd2).forEachIndexed { index, radioButton ->
            radioButton.setOnClickListener {
                step.setUndoAt(index)
            }
        }

        listOf<RadioButton>(rp1_rd3, rp2_rd3, rp3_rd3, rp4_rd3).forEachIndexed { index, radioButton ->
            radioButton.setOnClickListener {
                step.setDoingAt(index)
            }
        }

        listOf<RadioButton>(rp1_rd4, rp2_rd4, rp3_rd4, rp4_rd4).forEachIndexed { index, radioButton ->
            radioButton.setOnClickListener {
                step.setErrorAt(index)
            }
        }

        listOf<RadioButton>(rp1_rd5, rp2_rd5, rp3_rd5, rp4_rd5).forEachIndexed { index, radioButton ->
            radioButton.setOnClickListener {
                step.setDoneAt(index)
            }
        }

        rp6_rd1.setOnClickListener {
            step.setDirection(StepView.Direction.FromLeft)
        }
        rp6_rd2.setOnClickListener {
            step.setDirection(StepView.Direction.FromRight)
        }
        rp6_rd3.setOnClickListener {
            step.setDirection(StepView.Direction.FromTop)
        }
        rp6_rd4.setOnClickListener {
            step.setDirection(StepView.Direction.FromBottom)
        }
    }
}